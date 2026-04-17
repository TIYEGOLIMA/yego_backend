package com.yego.backend.service.yego_api_externo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_api_externo.api.request.YangoSummaryRequest;
import com.yego.backend.exception.YangoWeeklyException;
import com.yego.backend.entity.yego_api_externo.api.response.YangoIncomeSummary;
import com.yego.backend.entity.yego_api_externo.api.response.YangoMatchRow;
import com.yego.backend.entity.yego_api_externo.api.response.YangoPeriod;
import com.yego.backend.entity.yego_api_externo.api.response.YangoSuggestItem;
import com.yego.backend.entity.yego_api_externo.api.response.YangoSuggestionsBody;
import com.yego.backend.entity.yego_api_externo.api.response.YangoSummaryResponse;
import com.yego.backend.entity.yego_api_externo.api.response.YangoSummaryResponse.YangoIncomeBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class YangoWeeklyService {

    /** Hilos dedicados para llamadas HTTP a Yango (no bloquear ForkJoinPool.commonPool()). */
    private static final Executor YANGO_IO = Executors.newFixedThreadPool(6, r -> {
        Thread t = new Thread(r, "yango-external-io");
        t.setDaemon(true);
        return t;
    });

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter ISO_OFFSET =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final YangoClient yangoClient;
    private final ObjectMapper objectMapper;
    private final String incomeUrl;

    public YangoWeeklyService(
            YangoClient yangoClient,
            ObjectMapper objectMapper,
            @Value("${yego.yango.driver-income-url:https://fleet.yango.com/api/v1/cards/driver/income}") String incomeUrl) {
        this.yangoClient = yangoClient;
        this.objectMapper = objectMapper;
        this.incomeUrl = incomeUrl;
    }

    public YangoSummaryResponse summarize(YangoSummaryRequest req) {
        String parkId = req.getParkId() != null && !req.getParkId().isBlank()
                ? req.getParkId().trim()
                : YangoClient.DEFAULT_PARK_ID;

        String queryText = resolveQueryText(req);
        rejectNameOnlyLookup(queryText);
        LocalDate anchor = resolveAnchor(req);
        PeriodRange weeklyPeriod = resolveWeeklyPeriod(anchor);
        PeriodRange monthlyPeriod = resolveLastMonthPeriod(anchor);

        String contractorId = resolveContractor(queryText, parkId);

        CompletableFuture<YangoIncomeSummary> weeklyIncomeFuture =
                CompletableFuture.supplyAsync(() -> fetchIncome(contractorId, weeklyPeriod, parkId), YANGO_IO);

        CompletableFuture<YangoIncomeSummary> monthlyIncomeFuture =
                CompletableFuture.supplyAsync(() -> fetchIncome(contractorId, monthlyPeriod, parkId), YANGO_IO);

        CompletableFuture<GoalsResult> goalsFuture =
                CompletableFuture.supplyAsync(() -> fetchGoals(contractorId, parkId), YANGO_IO);

        YangoIncomeSummary weeklyIncome = weeklyIncomeFuture.join();
        YangoIncomeSummary monthlyIncome = monthlyIncomeFuture.join();
        GoalsResult goals = goalsFuture.join();

        return YangoSummaryResponse.builder()
                .resolvedContractorId(contractorId)
                .weekly(YangoIncomeBlock.builder()
                        .period(toPeriod(weeklyPeriod))
                        .incomeSummary(weeklyIncome)
                        .build())
                .monthly(YangoIncomeBlock.builder()
                        .period(toPeriod(monthlyPeriod))
                        .incomeSummary(monthlyIncome)
                        .build())
                .activeGoals(goals.active)
                .previousGoals(goals.previous)
                .build();
    }

    // ── resolvers ──

    private String resolveContractor(String queryText, String parkId) {
        YangoSuggestionsBody suggestionsBody;
        try {
            Map<String, Object> body = Map.of("query", Map.of("text", queryText));
            String json = objectMapper.writeValueAsString(body);
            ResponseEntity<String> resp = yangoClient.postSuggestions(json, parkId);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new YangoWeeklyException(
                        HttpStatus.BAD_GATEWAY, "YANGO_UPSTREAM_ERROR",
                        "Suggestions request failed: " + resp.getStatusCode());
            }
            suggestionsBody = objectMapper.readValue(resp.getBody(), YangoSuggestionsBody.class);
        } catch (YangoWeeklyException e) {
            throw e;
        } catch (Exception e) {
            log.error("[YangoWeeklyService] suggestions error: {}", e.getMessage());
            throw new YangoWeeklyException(
                    HttpStatus.BAD_GATEWAY, "YANGO_UPSTREAM_ERROR", "Upstream Yango error");
        }

        List<YangoSuggestItem> list =
                suggestionsBody.getSuggestions() != null ? suggestionsBody.getSuggestions() : List.of();

        List<YangoSuggestItem> withId = new ArrayList<>();
        for (YangoSuggestItem it : list) {
            if (it.getContractor() != null
                    && it.getContractor().getContractorId() != null
                    && !it.getContractor().getContractorId().isBlank()) {
                withId.add(it);
            }
        }

        if (withId.isEmpty()) {
            throw new YangoWeeklyException(
                    HttpStatus.NOT_FOUND, "CONTRACTOR_NOT_FOUND", "No contractor match for lookup");
        }

        if (withId.size() > 1) {
            List<YangoMatchRow> matches = new ArrayList<>();
            for (YangoSuggestItem it : withId) {
                YangoSuggestItem.YangoSuggestContractor c = it.getContractor();
                String name = formatName(c.getName());
                String plate = it.getVehicle() != null ? it.getVehicle().getNumber() : null;
                matches.add(YangoMatchRow.builder()
                        .contractorId(c.getContractorId())
                        .name(name)
                        .phone(c.getPhone())
                        .licensePlate(plate)
                        .build());
            }
            throw new YangoWeeklyException(
                    HttpStatus.CONFLICT, "MULTIPLE_MATCHES", "Multiple contractors matched", matches);
        }

        return withId.get(0).getContractor().getContractorId();
    }

    private YangoIncomeSummary fetchIncome(String contractorId, PeriodRange period, String parkId) {
        try {
            Map<String, Object> incomeBody = new HashMap<>();
            incomeBody.put("date_from", period.dateFrom);
            incomeBody.put("date_to", period.dateTo);
            incomeBody.put("driver_id", contractorId);
            String incomeJson = objectMapper.writeValueAsString(incomeBody);
            ResponseEntity<String> resp = yangoClient.postIncome(incomeUrl, incomeJson, parkId);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new YangoWeeklyException(
                        HttpStatus.BAD_GATEWAY, "YANGO_UPSTREAM_ERROR",
                        "Income request failed: " + resp.getStatusCode());
            }
            return mapIncome(objectMapper.readTree(resp.getBody()));
        } catch (YangoWeeklyException e) {
            throw e;
        } catch (Exception e) {
            log.error("[YangoWeeklyService] income error: {}", e.getMessage());
            throw new YangoWeeklyException(
                    HttpStatus.BAD_GATEWAY, "YANGO_UPSTREAM_ERROR", "Income upstream error");
        }
    }

    private GoalsResult fetchGoals(String contractorId, String parkId) {
        try {
            String goalsUrl = String.format(YangoClient.GOALS_URL_TEMPLATE, contractorId);
            ResponseEntity<String> resp = yangoClient.getGoals(goalsUrl, parkId);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                return new GoalsResult(
                        YangoGoalsJson.sanitizeGoalArray(root.get("active_goals")),
                        YangoGoalsJson.sanitizeGoalArray(root.get("previous_goals")));
            }
        } catch (Exception e) {
            log.warn("[YangoWeeklyService] goals skipped: {}", e.getMessage());
        }
        return new GoalsResult(new ArrayList<>(), new ArrayList<>());
    }

    private record GoalsResult(List<JsonNode> active, List<JsonNode> previous) {}

    // ── helpers ──

    /**
     * No permitir búsqueda solo por nombre/apellidos (texto sin dígitos ni otros identificadores).
     */
    private static void rejectNameOnlyLookup(String queryText) {
        String t = queryText.trim();
        if (t.length() < 2) {
            throw new YangoWeeklyException(
                    HttpStatus.BAD_REQUEST,
                    "NAME_LOOKUP_NOT_ALLOWED",
                    "No se permite buscar por nombre. Indica DNI, licencia, teléfono o ID de conductor.");
        }
        if (t.chars().anyMatch(Character::isDigit)) {
            return;
        }
        if (t.matches("^[\\p{L}\\s'.-]+$")) {
            throw new YangoWeeklyException(
                    HttpStatus.BAD_REQUEST,
                    "NAME_LOOKUP_NOT_ALLOWED",
                    "No se permite buscar por nombre. Indica DNI, licencia, teléfono o ID de conductor.");
        }
    }

    static String resolveQueryText(YangoSummaryRequest req) {
        if (req.getText() == null || req.getText().isBlank()) {
            throw new YangoWeeklyException(
                    HttpStatus.BAD_REQUEST, "INVALID_LOOKUP", "Field 'text' is required");
        }
        return req.getText().trim();
    }

    static LocalDate resolveAnchor(YangoSummaryRequest req) {
        try {
            return (req.getDate() != null && !req.getDate().isBlank())
                    ? LocalDate.parse(req.getDate().trim(), DAY)
                    : LocalDate.now(LIMA);
        } catch (DateTimeParseException e) {
            throw new YangoWeeklyException(
                    HttpStatus.BAD_REQUEST, "INVALID_DATE", "Invalid 'date' format, use yyyy-MM-dd");
        }
    }

    static PeriodRange resolveWeeklyPeriod(LocalDate anchor) {
        LocalDate monday = anchor.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        return new PeriodRange(
                monday.atStartOfDay(LIMA).format(ISO_OFFSET),
                sunday.atTime(23, 59, 59).atZone(LIMA).format(ISO_OFFSET));
    }

    static PeriodRange resolveLastMonthPeriod(LocalDate anchor) {
        YearMonth lastMonth = YearMonth.from(anchor).minusMonths(1);
        LocalDate firstDay = lastMonth.atDay(1);
        LocalDate lastDay = lastMonth.atEndOfMonth();
        return new PeriodRange(
                firstDay.atStartOfDay(LIMA).format(ISO_OFFSET),
                lastDay.atTime(23, 59, 59).atZone(LIMA).format(ISO_OFFSET));
    }

    private static YangoPeriod toPeriod(PeriodRange p) {
        return YangoPeriod.builder().dateFrom(p.dateFrom).dateTo(p.dateTo).build();
    }

    private static String formatName(YangoSuggestItem.YangoName name) {
        if (name == null) return null;
        String f = name.getFirst() != null ? name.getFirst() : "";
        String l = name.getLast() != null ? name.getLast() : "";
        String s = (f + " " + l).trim();
        return s.isEmpty() ? null : s;
    }

    private static YangoIncomeSummary mapIncome(JsonNode root) {
        JsonNode orders = root.get("orders");
        JsonNode balances = root.get("balances");

        return YangoIncomeSummary.builder()
                .countCompleted(intOr0(orders, "count_completed"))
                .total(doubleOrNullRounded(balances, "total"))
                .cashCollected(r2(doubleOr0(balances, "cash_collected")))
                .nonCashPayment(r2(doubleOr0(balances, "platform_card") + doubleOr0(balances, "partner_cashless")))
                .corporate(r2(doubleOr0(balances, "platform_corporate")))
                .promotionCompensation(r2(doubleOr0(balances, "platform_promotion")))
                .build();
    }

    private static double r2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static int intOr0(JsonNode parent, String field) {
        if (parent == null || !parent.has(field) || parent.get(field).isNull()) return 0;
        return parent.get(field).asInt(0);
    }

    private static double doubleOr0(JsonNode parent, String field) {
        if (parent == null || !parent.has(field) || parent.get(field).isNull()) return 0.0;
        return parent.get(field).asDouble(0.0);
    }

    private static Double doubleOrNullRounded(JsonNode parent, String field) {
        if (parent == null || !parent.has(field) || parent.get(field).isNull()) return null;
        return r2(parent.get(field).asDouble(0.0));
    }

    record PeriodRange(String dateFrom, String dateTo) {}
}
