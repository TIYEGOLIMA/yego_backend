package com.yego.backend.service.yego_api_externo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_api_externo.api.request.YangoSummaryRequest;
import com.yego.backend.entity.yego_api_externo.api.response.YangoDriverSnapshot;
import com.yego.backend.entity.yego_api_externo.api.response.YangoIncomeSummary;
import com.yego.backend.entity.yego_api_externo.api.response.YangoMatchRow;
import com.yego.backend.entity.yego_api_externo.api.response.YangoPeriod;
import com.yego.backend.entity.yego_api_externo.api.response.YangoSuggestItem;
import com.yego.backend.entity.yego_api_externo.api.response.YangoSuggestionsBody;
import com.yego.backend.entity.yego_api_externo.api.response.YangoSummaryResponse;
import com.yego.backend.entity.yego_api_externo.api.response.YangoSummaryResponse.YangoIncomeBlock;
import com.yego.backend.exception.YangoWeeklyException;
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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class YangoWeeklyService {

    /**
     * Hilos dedicados para llamadas HTTP a Yango (no bloquear ForkJoinPool.commonPool()).
     * Por consulta lanzamos hasta 6 llamadas en paralelo (weekly, monthly + resta objetivo transactions, goals,
     * details, common).
     */
    private static final Executor YANGO_IO = Executors.newFixedThreadPool(16, r -> {
        Thread t = new Thread(r, "yango-external-io");
        t.setDaemon(true);
        return t;
    });

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter ISO_OFFSET =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String OBJECTIVE_BONUS_DESC = "Bonificación por cumplir objetivo";
    static final String DEFAULT_PARK_ID = YangoClient.DEFAULT_PARK_ID;

    private final YangoClient yangoClient;
    private final ObjectMapper objectMapper;
    private final String incomeUrl;
    private final String driverDetailsUrl;
    private final String driverCommonUrl;
    private final String driverTransactionsListUrl;

    public YangoWeeklyService(
            YangoClient yangoClient,
            ObjectMapper objectMapper,
            @Value("${yego.yango.driver-income-url:https://fleet.yango.com/api/v1/cards/driver/income}") String incomeUrl,
            @Value("${yego.yango.driver-details-url:https://fleet.yango.com/api/v1/cards/driver/details}") String driverDetailsUrl,
            @Value("${yego.yango.driver-common-url:https://fleet.yango.com/api/fleet/router/v1/cards/driver/common}") String driverCommonUrl,
            @Value("${yego.yango.driver-transactions-list-url:https://fleet.yango.com/api/v1/reports/transactions/driver/list}")
                    String driverTransactionsListUrl) {
        this.yangoClient = yangoClient;
        this.objectMapper = objectMapper;
        this.incomeUrl = incomeUrl;
        this.driverDetailsUrl = driverDetailsUrl;
        this.driverCommonUrl = driverCommonUrl;
        this.driverTransactionsListUrl = driverTransactionsListUrl;
    }

    public YangoSummaryResponse summarize(YangoSummaryRequest req) {
        String parkId = req.getParkId() != null && !req.getParkId().isBlank()
                ? req.getParkId().trim()
                : YangoClient.DEFAULT_PARK_ID;

        LocalDate anchor = resolveAnchor(req);
        PeriodRange weeklyPeriod = resolveWeeklyPeriod(anchor);
        PeriodRange monthlyPeriod = resolveLastMonthPeriod(anchor);

        String contractorId = resolveContractorId(req, parkId);

        log.info(
                "[YangoWeeklyService] Yango upstream: income + monthly + POST {} (Bonificación cumplir objetivo → restar)"
                        + " + goals + details + common. parkId={} contractorId={}",
                driverTransactionsListUrl,
                parkId,
                contractorId);

        // Todas las llamadas a Yango se disparan en paralelo en el mismo nivel (no anidadas).
        CompletableFuture<YangoIncomeSummary> weeklyIncomeFuture =
                CompletableFuture.supplyAsync(() -> fetchIncome(contractorId, weeklyPeriod, parkId), YANGO_IO);

        CompletableFuture<YangoIncomeSummary> monthlyIncomeFuture =
                CompletableFuture.supplyAsync(() -> fetchIncome(contractorId, monthlyPeriod, parkId), YANGO_IO);

        CompletableFuture<Optional<Double>> objetivoSubtractFuture =
                CompletableFuture.supplyAsync(
                        () -> tryFetchSumAmountBonificacionCumplirObjetivo(contractorId, parkId, weeklyPeriod),
                        YANGO_IO);

        CompletableFuture<GoalsResult> goalsFuture =
                CompletableFuture.supplyAsync(() -> fetchGoals(contractorId, parkId), YANGO_IO);

        CompletableFuture<JsonNode> detailsFuture =
                CompletableFuture.supplyAsync(
                        () -> postDriverCardJson(driverDetailsUrl, contractorId, parkId, "details"),
                        YANGO_IO);

        CompletableFuture<JsonNode> commonFuture =
                CompletableFuture.supplyAsync(
                        () -> postDriverCardJson(driverCommonUrl, contractorId, parkId, "common"),
                        YANGO_IO);

        CompletableFuture.allOf(
                        weeklyIncomeFuture,
                        monthlyIncomeFuture,
                        objetivoSubtractFuture,
                        goalsFuture,
                        detailsFuture,
                        commonFuture)
                .join();

        YangoIncomeSummary weeklyIncome = weeklyIncomeFuture.join();
        Optional<Double> restaBonifObjetivo = objetivoSubtractFuture.join();
        if (restaBonifObjetivo.isPresent()) {
            double bonifTarjeta = d0(weeklyIncome != null ? weeklyIncome.getBonificacion() : null);
            double resta = restaBonifObjetivo.get();
            double nuevaBonif = r2(bonifTarjeta - resta);
            log.info(
                    "[YangoWeeklyService] platform_bonus (tarjeta)={} menos sum(amount) líneas «Bonificación por cumplir objetivo»={} → bonificación semanal={}",
                    bonifTarjeta,
                    resta,
                    nuevaBonif);
            weeklyIncome = withWeeklyBonificacionOnly(weeklyIncome, nuevaBonif);
        } else {
            log.debug(
                    "[YangoWeeklyService] Sin LIST transactions o error; bonificación semanal = sólo tarjeta income.");
        }

        YangoIncomeSummary monthlyIncome = monthlyIncomeFuture.join();
        GoalsResult goals = goalsFuture.join();
        YangoDriverSnapshot driverSnapshot = buildDriverSnapshot(detailsFuture.join(), commonFuture.join());

        return YangoSummaryResponse.builder()
                .resolvedContractorId(contractorId)
                .driver(driverSnapshot)
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

    /**
     * Obtiene únicamente la bonificación semanal corregida para un conductor.
     * Realiza dos llamadas en paralelo: tarjeta income (platform_bonus) y listado de
     * transacciones (para restar «Bonificación por cumplir objetivo»).
     *
     * @return Optional con el valor corregido de bonificación, o {@code Optional.empty()} si falla.
     */
    public Optional<Double> fetchCorrectedWeeklyBonus(String driverProfileId, String parkId,
                                                       String dateFrom, String dateTo) {
        Optional<YangoIncomeSummary> summary =
                fetchCorrectedWeeklyIncomeSummary(driverProfileId, parkId, dateFrom, dateTo);
        return summary.map(YangoIncomeSummary::getBonificacion);
    }

    /**
     * Obtiene el resumen de ingresos semanal completo con la bonificación ya corregida
     * (restando «Bonificación por cumplir objetivo» del platform_bonus de la tarjeta income).
     *
     * @return Optional con el YangoIncomeSummary corregido, o {@code Optional.empty()} si falla.
     */
    public Optional<YangoIncomeSummary> fetchCorrectedWeeklyIncomeSummary(
            String driverProfileId, String parkId, String dateFrom, String dateTo) {
        PeriodRange week = new PeriodRange(dateFrom, dateTo);
        String pid = parkId != null && !parkId.isBlank() ? parkId.trim() : DEFAULT_PARK_ID;

        CompletableFuture<YangoIncomeSummary> incomeFuture =
                CompletableFuture.supplyAsync(() -> fetchIncome(driverProfileId, week, pid), YANGO_IO);

        CompletableFuture<Optional<Double>> subtractFuture =
                CompletableFuture.supplyAsync(
                        () -> tryFetchSumAmountBonificacionCumplirObjetivo(driverProfileId, pid, week),
                        YANGO_IO);

        CompletableFuture.allOf(incomeFuture, subtractFuture).join();

        YangoIncomeSummary weeklyIncome = incomeFuture.join();
        if (weeklyIncome == null) {
            return Optional.empty();
        }

        Optional<Double> restaBonifObjetivo = subtractFuture.join();
        if (restaBonifObjetivo.isPresent()) {
            double bonifTarjeta = d0(weeklyIncome.getBonificacion());
            double resta = restaBonifObjetivo.get();
            double nuevaBonif = r2(bonifTarjeta - resta);
            log.info(
                    "[YangoWeeklyService] fetchCorrectedWeeklyIncomeSummary driver={} platform_bonus={} - resta={} → {}",
                    driverProfileId, bonifTarjeta, resta, nuevaBonif);
            weeklyIncome = withWeeklyBonificacionOnly(weeklyIncome, nuevaBonif);
        } else {
            log.debug("[YangoWeeklyService] fetchCorrectedWeeklyIncomeSummary driver={} sin transacciones, usando tarjeta tal cual",
                    driverProfileId);
        }
        return Optional.of(weeklyIncome);
    }

    public Optional<Double> fetchFirstBonusTransactionAmount(
            String driverProfileId, String parkId, String date) {
        PeriodRange range = new PeriodRange(
                date + "T00:00:00-05:00",
                date + "T23:59:59-05:00");
        return tryFetchFirstBonusTransaction(driverProfileId, parkId, range);
    }

    private Optional<Double> tryFetchFirstBonusTransaction(
            String driverProfileId, String parkId, PeriodRange range) {
        Map<String, Object> txn = new LinkedHashMap<>();
        txn.put("event_at", Map.of("from", range.dateFrom, "to", range.dateTo));
        txn.put("category_ids", List.of("bonus", "bonus_discount", "platform_bonus_fee"));
        Map<String, Object> park = new LinkedHashMap<>();
        park.put("driver_profile", Map.of("id", driverProfileId));
        park.put("transaction", txn);
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("park", park);

        String nextCursor = null;
        try {
            do {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("query", query);
                if (nextCursor != null && !nextCursor.isBlank()) {
                    body.put("cursor", nextCursor);
                }
                String payload = objectMapper.writeValueAsString(body);
                ResponseEntity<String> resp = yangoClient.postFleet(driverTransactionsListUrl, payload, parkId);
                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                    return Optional.empty();
                }
                JsonNode root = objectMapper.readTree(resp.getBody());
                JsonNode list = root.get("transactions");
                if (list != null && list.isArray()) {
                    for (JsonNode tx : list) {
                        String desc = readTransactionDescription(tx);
                        if (!isObjectiveCompletionBonusDescription(desc)) {
                            continue;
                        }
                        double amount = readTransactionAmount(tx);
                        if (amount > 100) {
                            return Optional.of(amount);
                        }
                    }
                }
                nextCursor = root.hasNonNull("cursor") && root.get("cursor").isTextual()
                        ? root.get("cursor").asText().trim() : "";
                if (nextCursor.isBlank()) nextCursor = null;
            } while (nextCursor != null);
        } catch (Exception e) {
            log.warn("[YangoWeeklyService] fetchFirstBonusTransaction error: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Obtiene el resumen de ingresos semanal completo TAL CUAL lo reporta Yango,
     * sin restar ningún concepto. Útil para facturación donde el bono se cuenta completo.
     *
     * @return Optional con el YangoIncomeSummary crudo, o {@code Optional.empty()} si falla.
     */
    public Optional<YangoIncomeSummary> fetchWeeklyIncomeSummary(
            String driverProfileId, String parkId, String dateFrom, String dateTo) {
        PeriodRange week = new PeriodRange(dateFrom, dateTo);
        String pid = parkId != null && !parkId.isBlank() ? parkId.trim() : DEFAULT_PARK_ID;
        try {
            YangoIncomeSummary income = fetchIncome(driverProfileId, week, pid);
            if (income != null) {
                log.info("[YangoWeeklyService] fetchWeeklyIncomeSummary driver={} platform_bonus={} cash={}",
                        driverProfileId, income.getBonificacion(), income.getCashCollected());
            }
            return Optional.ofNullable(income);
        } catch (Exception e) {
            log.warn("[YangoWeeklyService] fetchWeeklyIncomeSummary driver={} error: {}",
                    driverProfileId, e.getMessage());
            return Optional.empty();
        }
    }

    // ── resolvers ──

    /**
     * Resuelve el ID de contratista vía {@code contractor_id} directo (rápido) o búsqueda suggestions.
     */
    private String resolveContractorId(YangoSummaryRequest req, String parkId) {
        if (req.getContractorId() != null && !req.getContractorId().isBlank()) {
            return req.getContractorId().trim();
        }
        String queryText = resolveQueryText(req);
        rejectNameOnlyLookup(queryText);
        return resolveContractor(queryText, parkId);
    }

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
            ResponseEntity<String> resp = yangoClient.postFleet(incomeUrl, incomeJson, parkId);
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

    /**
     * Una sola llamada a {@code reports/transactions/driver/list}: mismo rango semanal que la tarjeta income,
     * {@code driver_profile.id} = ID de conductor ya resuelto. Suma {@code amount} solo si la fila coincide con
     * «Bonificación por cumplir objetivo» y esa suma se resta después de {@code platform_bonus}.
     */
    private Optional<Double> tryFetchSumAmountBonificacionCumplirObjetivo(
            String driverProfileId, String parkId, PeriodRange weeklySameAsIncome) {
        Map<String, Object> txn = new LinkedHashMap<>();
        txn.put("event_at", Map.of("from", weeklySameAsIncome.dateFrom, "to", weeklySameAsIncome.dateTo));
        txn.put("category_ids", List.of("bonus", "bonus_discount", "platform_bonus_fee"));
        Map<String, Object> park = new LinkedHashMap<>();
        park.put("driver_profile", Map.of("id", driverProfileId));
        park.put("transaction", txn);
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("park", park);

        double sumAmountBonifCumplirObjetivo = 0.0;
        int filasTextoObjetivo = 0;
        String nextCursor = null;
        String ultimoPayload = null;
        try {
            log.info(
                    "[YangoWeeklyService] POST driver transactions list driver_profile.id={} event_at=[{}, {}]",
                    driverProfileId,
                    weeklySameAsIncome.dateFrom,
                    weeklySameAsIncome.dateTo);
            do {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("query", query);
                if (nextCursor != null && !nextCursor.isBlank()) {
                    body.put("cursor", nextCursor);
                }
                ultimoPayload = objectMapper.writeValueAsString(body);
                ResponseEntity<String> resp = yangoClient.postFleet(driverTransactionsListUrl, ultimoPayload, parkId);
                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                    log.warn(
                            "[YangoWeeklyService] driver transactions list HTTP {} — payload={}",
                            resp.getStatusCode(),
                            ultimoPayload);
                    return Optional.empty();
                }
                JsonNode root = objectMapper.readTree(resp.getBody());
                JsonNode list = root.get("transactions");
                if (list != null && list.isArray()) {
                    for (JsonNode tx : list) {
                        String desc = readTransactionDescription(tx);
                        if (!isObjectiveCompletionBonusDescription(desc)) {
                            continue;
                        }
                        sumAmountBonifCumplirObjetivo += readTransactionAmount(tx);
                        filasTextoObjetivo++;
                    }
                }
                nextCursor =
                        root.hasNonNull("cursor") && root.get("cursor").isTextual()
                                ? root.get("cursor").asText().trim()
                                : "";
                if (nextCursor.isBlank()) {
                    nextCursor = null;
                }
            } while (nextCursor != null);
            double suma = r2(sumAmountBonifCumplirObjetivo);
            log.info(
                    "[YangoWeeklyService] Listado: #filas «Bonificación por cumplir objetivo»={} sum(amount) a restar de platform_bonus={}",
                    filasTextoObjetivo,
                    suma);
            return Optional.of(suma);
        } catch (Exception e) {
            log.warn(
                    "[YangoWeeklyService] driver transactions list error: {} payload={}",
                    e.getMessage(),
                    ultimoPayload);
            return Optional.empty();
        }
    }

    /** Lee {@code amount} tal como viene en Fleet (número, texto o objeto con valor numérico). */
    private static double readTransactionAmount(JsonNode tx) {
        if (tx == null || !tx.has("amount") || tx.get("amount").isNull()) {
            return 0.0;
        }
        JsonNode a = tx.get("amount");
        if (a.isNumber()) {
            return a.asDouble();
        }
        if (a.isTextual()) {
            return parseAmountText(a);
        }
        if (a.isObject()) {
            JsonNode value = a.get("value");
            if (value != null && value.isNumber()) {
                return value.asDouble();
            }
            if (value != null && value.isTextual()) {
                return parseAmountText(value);
            }
        }
        return 0.0;
    }

    private static String readTransactionDescription(JsonNode tx) {
        if (tx == null || !tx.hasNonNull("description")) {
            return "";
        }
        JsonNode d = tx.get("description");
        return d.isTextual() ? d.asText().trim() : "";
    }

    private static double parseAmountText(JsonNode n) {
        if (n == null || !n.isTextual()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(n.asText().trim().replace(",", "."));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static boolean isObjectiveCompletionBonusDescription(String description) {
        return OBJECTIVE_BONUS_DESC.equals(description);
    }

    /** Solo sobrescribe {@code bonificacion}; el resto del resumen permanece igual al de la tarjeta income. */
    private static YangoIncomeSummary withWeeklyBonificacionOnly(YangoIncomeSummary income, double newBonificacion) {
        if (income == null) {
            return null;
        }
        double b = r2(newBonificacion);
        double newTotal = r2(income.getCashCollected()
                + income.getNonCashPayment()
                + income.getCorporate()
                + income.getPromotionCompensation()
                + b
                + income.getTips()
                + income.getPlatformFees()
                + income.getPartnerFees()
                + income.getPlatformGas()
                + income.getPlatformOther()
                + income.getMandatoryTaxesFee()
                + income.getPlatformMarketingOther()
                + income.getPartnerContractorOther());
        double newPriceYangoPro = r2(income.getCashCollected()
                + income.getNonCashPayment()
                + income.getCorporate()
                + income.getPromotionCompensation()
                + b
                + income.getTips());
        return YangoIncomeSummary.builder()
                .countCompleted(income.getCountCompleted())
                .total(newTotal)
                .cashCollected(income.getCashCollected())
                .nonCashPayment(income.getNonCashPayment())
                .corporate(income.getCorporate())
                .promotionCompensation(income.getPromotionCompensation())
                .bonificacion(b)
                .tips(income.getTips())
                .platformFees(income.getPlatformFees())
                .partnerFees(income.getPartnerFees())
                .platformGas(income.getPlatformGas())
                .platformOther(income.getPlatformOther())
                .mandatoryTaxesFee(income.getMandatoryTaxesFee())
                .platformMarketingOther(income.getPlatformMarketingOther())
                .partnerContractorOther(income.getPartnerContractorOther())
                .priceYangoPro(newPriceYangoPro)
                .build();
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

    /** details: perfil y saldo; common: calificación (rating no suele venir en details). */
    private static YangoDriverSnapshot buildDriverSnapshot(JsonNode details, JsonNode common) {
        YangoDriverSnapshot fromDetails = mapDriverFromDetailsNode(details);
        if (fromDetails != null) {
            Double rating = readAverageRating(common != null ? common.get("driver") : null);
            if (rating != null) {
                fromDetails.setAverageRating(rating);
            }
            return fromDetails;
        }
        return mapDriverFromCommonNode(common);
    }

    private JsonNode postDriverCardJson(String url, String contractorId, String parkId, String logLabel) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("driver_id", contractorId));
            ResponseEntity<String> resp = yangoClient.postFleet(url, json, parkId);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return objectMapper.readTree(resp.getBody());
            }
        } catch (Exception e) {
            log.warn("[YangoWeeklyService] driver card {} error: {}", logLabel, e.getMessage());
        }
        return null;
    }

    private static YangoDriverSnapshot mapDriverFromDetailsNode(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode d = root.get("driver");
        if (d == null) {
            return null;
        }
        JsonNode profile = d.get("driver_profile");
        String driverId = textAt(profile, "id");
        String first = textAt(profile, "first_name");
        String last = textAt(profile, "last_name");
        String full = joinNames(first, last);
        String phone = firstPhone(profile != null ? profile.get("phones") : null);
        String license = null;
        if (profile != null && profile.has("license") && !profile.get("license").isNull()) {
            license = textAt(profile.get("license"), "number");
        }
        AccountFields acc = pickCurrentAccount(d.get("accounts"));

        if (driverId == null
                && first == null
                && last == null
                && phone == null
                && license == null
                && acc == null) {
            return null;
        }

        return YangoDriverSnapshot.builder()
                .driverId(driverId)
                .firstName(first)
                .lastName(last)
                .fullName(full)
                .phone(phone)
                .licenseNumber(license)
                .balance(acc != null ? acc.balance() : null)
                .balanceLimit(acc != null ? acc.balanceLimit() : null)
                .currency(acc != null ? acc.currency() : null)
                .build();
    }

    private static YangoDriverSnapshot mapDriverFromCommonNode(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode d = root.get("driver");
        if (d == null) {
            return null;
        }
        JsonNode profile = d.get("driver_profile");
        String driverId = textAt(profile, "id");
        String first = textAt(profile, "first_name");
        String last = textAt(profile, "last_name");
        String full = joinNames(first, last);
        String license = null;
        if (profile != null && profile.has("license") && !profile.get("license").isNull()) {
            license = textAt(profile.get("license"), "number");
        }
        AccountFields acc = pickCurrentAccount(d.get("accounts"));
        Double rating = readAverageRating(d);

        if (driverId == null
                && first == null
                && last == null
                && license == null
                && acc == null
                && rating == null) {
            return null;
        }

        return YangoDriverSnapshot.builder()
                .driverId(driverId)
                .firstName(first)
                .lastName(last)
                .fullName(full)
                .licenseNumber(license)
                .balance(acc != null ? acc.balance() : null)
                .balanceLimit(acc != null ? acc.balanceLimit() : null)
                .currency(acc != null ? acc.currency() : null)
                .averageRating(rating)
                .build();
    }

    private static Double readAverageRating(JsonNode driver) {
        if (driver == null) {
            return null;
        }
        JsonNode r = driver.get("rating");
        if (r == null) {
            return null;
        }
        JsonNode ar = r.get("average_rating");
        if (ar == null || ar.isNull()) {
            return null;
        }
        if (ar.isNumber()) {
            return ar.asDouble();
        }
        if (ar.isTextual()) {
            try {
                return Double.parseDouble(ar.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String textAt(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) {
            return null;
        }
        String t = n.get(field).asText();
        return t != null && !t.isBlank() ? t.trim() : null;
    }

    private static String firstPhone(JsonNode phones) {
        if (phones == null || !phones.isArray() || phones.isEmpty()) {
            return null;
        }
        JsonNode z = phones.get(0);
        if (z == null || z.isNull()) {
            return null;
        }
        String t = z.isTextual() ? z.asText() : null;
        return t != null && !t.isBlank() ? t.trim() : null;
    }

    private static String joinNames(String first, String last) {
        String s = (first != null ? first : "") + " " + (last != null ? last : "");
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private record AccountFields(Double balance, Double balanceLimit, String currency) {}

    private static AccountFields pickCurrentAccount(JsonNode accounts) {
        if (accounts == null || !accounts.isArray() || accounts.isEmpty()) {
            return null;
        }
        for (int i = 0; i < accounts.size(); i++) {
            JsonNode a = accounts.get(i);
            if (a == null) {
                continue;
            }
            String type = a.has("type") && a.get("type").isTextual() ? a.get("type").asText() : null;
            if ("current".equalsIgnoreCase(type)) {
                return toAccountFields(a);
            }
        }
        return toAccountFields(accounts.get(0));
    }

    private static AccountFields toAccountFields(JsonNode a) {
        if (a == null) {
            return null;
        }
        Double b = a.has("balance") && a.get("balance").isNumber() ? a.get("balance").asDouble() : null;
        Double bl =
                a.has("balance_limit") && a.get("balance_limit").isNumber()
                        ? a.get("balance_limit").asDouble()
                        : null;
        String cur = textAt(a, "currency");
        if (b == null && bl == null && (cur == null || cur.isBlank())) {
            return null;
        }
        return new AccountFields(b, bl, cur);
    }

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

        double cashCollected = r2(doubleOr0(balances, "cash_collected"));
        double nonCashPayment =
                r2(doubleOr0(balances, "platform_card") + doubleOr0(balances, "partner_cashless"));
        double corporate = r2(doubleOr0(balances, "platform_corporate"));
        double promotionCompensation = r2(doubleOr0(balances, "platform_promotion"));
        double bonificacion = r2(doubleOr0(balances, "platform_bonus"));
        double tips = r2(tipsFromBalances(balances));
        double platformFees = r2(doubleOr0(balances, "platform_fees"));
        double partnerFees = r2(doubleOr0(balances, "partner_fees"));
        double platformGas = r2(doubleOr0(balances, "platform_gas"));
        double platformOther = r2(doubleOr0(balances, "platform_other"));
        double mandatoryTaxesFee = r2(doubleOr0(balances, "mandatory_taxes_fee"));
        double platformMarketingOther = r2(doubleOr0(balances, "platform_marketing_other"));
        double partnerContractorOther = r2(doubleOr0(balances, "partner_contractor_other"));
        double priceYangoPro =
                r2(cashCollected
                        + nonCashPayment
                        + corporate
                        + tips
                        + promotionCompensation
                        + bonificacion);

        return YangoIncomeSummary.builder()
                .countCompleted(intOr0(orders, "count_completed"))
                .total(doubleOrNullRounded(balances, "total"))
                .cashCollected(cashCollected)
                .nonCashPayment(nonCashPayment)
                .corporate(corporate)
                .promotionCompensation(promotionCompensation)
                .bonificacion(bonificacion)
                .tips(tips)
                .platformFees(platformFees)
                .partnerFees(partnerFees)
                .platformGas(platformGas)
                .platformOther(platformOther)
                .mandatoryTaxesFee(mandatoryTaxesFee)
                .platformMarketingOther(platformMarketingOther)
                .partnerContractorOther(partnerContractorOther)
                .priceYangoPro(priceYangoPro)
                .build();
    }

    /** Suma de campos de propinas habituales en {@code balances} de la tarjeta de ingresos. */
    private static double tipsFromBalances(JsonNode balances) {
        if (balances == null) {
            return 0.0;
        }
        return doubleOr0(balances, "tip")
                + doubleOr0(balances, "platform_tip")
                + doubleOr0(balances, "partner_ride_tip");
    }

    private static double r2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double d0(Double v) {
        return v == null ? 0.0 : v;
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
