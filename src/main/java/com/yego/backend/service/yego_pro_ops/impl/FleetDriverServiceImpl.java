package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.config.yego_pro_ops.ProxyConfig;
import com.yego.backend.config.yego_pro_ops.YegoProOpsProperties;
import com.yego.backend.integration.YangoCookiePool;
import com.yego.backend.entity.yego_pro_ops.api.request.DriverListRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.ContractorSuggestionsResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverSimpleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FleetDriverServiceImpl extends BaseYangoApiService implements FleetDriverService {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private static final long DRIVERS_IN_ORDER_CACHE_TTL_MS = 45_000L;
    private static final long DRIVERS_LIST_SIMPLE_CACHE_TTL_MS = 90_000L;
    private static final long CONTRACTOR_SUGGESTIONS_CACHE_TTL_MS = 90_000L;
    private static final int EXECUTOR_THREADS = 20;
    private static final int PARALLEL_STREAM_THRESHOLD = 8;
    private static final int DETAILS_TIMEOUT_SECONDS = 30;

    private final DriverOrdersService driverOrdersService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(EXECUTOR_THREADS);

    private final Map<String, Cached<DriversInOrderResponse>> driversInOrderCache = new ConcurrentHashMap<>();
    private volatile Cached<DriversListSnapshot> driversListCache;
    private volatile Cached<DriverListResponse> driverListRawCache;
    private final Map<String, Cached<ContractorSuggestionsResponse>> contractorSuggestionsCache = new ConcurrentHashMap<>();

    public FleetDriverServiceImpl(
            RestTemplate restTemplate,
            @Qualifier("yangoProxyRestTemplate") RestTemplate yangoProxyRestTemplate,
            ProxyConfig proxyConfig,
            YangoCookiePool cookiePool,
            ObjectMapper objectMapper,
            YegoProOpsProperties proOpsProperties,
            DriverOrdersService driverOrdersService) {
        super(restTemplate, yangoProxyRestTemplate, proxyConfig, cookiePool, objectMapper, proOpsProperties);
        this.driverOrdersService = driverOrdersService;
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public DriverListResponse obtenerListaConductores() {
        Cached<DriverListResponse> cached = driverListRawCache;
        if (cached != null && !cached.expired()) {
            return cached.value();
        }
        try {
            String requestJson = objectMapper.writeValueAsString(crearDriverListRequest());

            ResponseEntity<String> response = ejecutarConRetryCookies(
                proOpsProperties.getYango().getContractorsUrl(),
                HttpMethod.POST,
                requestJson,
                this::crearHeadersConCookie
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                DriverListResponse result = transformarAListaConductores(jsonResponse);
                if (result != null) {
                    driverListRawCache = Cached.of(result, DRIVERS_LIST_SIMPLE_CACHE_TTL_MS);
                }
                return result;
            }
            return DriverListResponse.builder().contractors(new ArrayList<>()).build();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[FleetDriverService] HTTP {} obteniendo lista conductores: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[FleetDriverService] error obteniendo lista conductores: {}", e.getMessage(), e);
        }
        return DriverListResponse.builder().contractors(new ArrayList<>()).build();
    }

    @Override
    public DriverSimpleResponse obtenerListaConductoresSimplificada() {
        try {
            DriverListResponse driverList = obtenerListaConductores();
            if (driverList == null || driverList.getContractors() == null) {
                return DriverSimpleResponse.builder().conductores(new ArrayList<>()).build();
            }

            List<DriverSimpleResponse.DriverInfo> conductores = driverList.getContractors().stream()
                .filter(c -> c.getId() != null && !c.getId().isEmpty())
                .map(c -> DriverSimpleResponse.DriverInfo.builder()
                    .driverId(c.getId())
                    .nombre(c.getFullName())
                    .telefono(c.getPhone())
                    .avatarUrl(c.getAvatarUrl())
                    .build())
                .collect(Collectors.toList());

            return DriverSimpleResponse.builder().conductores(conductores).build();
        } catch (Exception e) {
            log.error("[FleetDriverService] error lista conductores simplificada: {}", e.getMessage(), e);
            return DriverSimpleResponse.builder().conductores(new ArrayList<>()).build();
        }
    }

    private DriverListRequest crearDriverListRequest() {
        return DriverListRequest.builder()
            .filter(new HashMap<>())
            .limit(50)
            .projection(List.of("id", "full_name", "phone", "avatar_url"))
            .build();
    }

    private DriverListResponse transformarAListaConductores(JsonNode jsonResponse) {
        List<DriverListResponse.ContractorResponse> contractors = new ArrayList<>();
        JsonNode contractorsNode = jsonResponse.get("contractors");
        if (contractorsNode == null || !contractorsNode.isArray()) {
            return DriverListResponse.builder().contractors(contractors).build();
        }
        for (JsonNode contractorNode : contractorsNode) {
            DriverListResponse.ContractorResponse contractor = mapearContractor(contractorNode);
            if (contractor != null) contractors.add(contractor);
        }
        return DriverListResponse.builder().contractors(contractors).build();
    }

    private DriverListResponse.ContractorResponse mapearContractor(JsonNode node) {
        try {
            return DriverListResponse.ContractorResponse.builder()
                .id(obtenerTexto(node, "id"))
                .avatarUrl(obtenerTexto(node, "avatar_url"))
                .fullName(obtenerTexto(node, "full_name"))
                .phone(obtenerTexto(node, "phone"))
                .build();
        } catch (Exception e) {
            log.error("[FleetDriverService] error mapeando contractor: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public DriversInOrderResponse obtenerConductoresEnOrden(Integer page, Integer limit) {
        long startTime = System.currentTimeMillis();
        String cacheKey = page + ":" + limit;
        Cached<DriversInOrderResponse> cached = driversInOrderCache.get(cacheKey);
        if (cached != null && !cached.expired()) {
            return cached.value();
        }
        try {
            DriversListSnapshot snapshot = obtenerOActualizarDriversList();
            if (snapshot.driverIds().isEmpty()) {
                return crearRespuestaVaciaConductores();
            }
            int total = snapshot.driverIds().size();
            PaginationResult pagination = aplicarPaginacion(snapshot.driverIds(), snapshot.balanceMap(), page, limit, total);
            if (pagination == null) {
                return DriversInOrderResponse.builder().conductores(new ArrayList<>()).total(total).build();
            }

            DriversInOrderResponse result = obtenerDetallesConductores(pagination.driverIds(), pagination.balanceMap());
            if (result == null || result.getConductores() == null || result.getConductores().isEmpty()) {
                result = DriversInOrderResponse.builder()
                    .conductores(crearConductoresBasicos(pagination.driverIds(), pagination.balanceMap()))
                    .total(total)
                    .build();
            } else {
                result.setTotal(total);
            }
            driversInOrderCache.put(cacheKey, Cached.of(result, DRIVERS_IN_ORDER_CACHE_TTL_MS));
            log.debug("[FleetDriverService] conductoresEnOrden page={} limit={} total={} ({}ms)",
                page, limit, total, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("[FleetDriverService] error conductoresEnOrden ({}ms): {}",
                System.currentTimeMillis() - startTime, e.getMessage(), e);
            return crearRespuestaVaciaConductores();
        }
    }

    private DriversListSnapshot obtenerOActualizarDriversList() {
        Cached<DriversListSnapshot> cache = driversListCache;
        if (cache != null && !cache.expired()) {
            DriversListSnapshot snap = cache.value();
            return new DriversListSnapshot(snap.driverIds(), new HashMap<>(snap.balanceMap()));
        }
        warmupCookiePool();
        JsonNode items = obtenerConductoresInOrderDesdeAPI();
        if (items == null || !items.isArray() || items.isEmpty()) {
            return new DriversListSnapshot(List.of(), new HashMap<>());
        }
        Map<String, Double> balanceMap = new HashMap<>();
        List<String> driverIds = extraerDriverIdsYBalances(items, balanceMap);
        if (driverIds.isEmpty()) {
            return new DriversListSnapshot(List.of(), new HashMap<>());
        }
        DriversListSnapshot snapshot = new DriversListSnapshot(
            new ArrayList<>(driverIds), new HashMap<>(balanceMap));
        driversListCache = Cached.of(snapshot, DRIVERS_IN_ORDER_CACHE_TTL_MS);
        return snapshot;
    }

    private JsonNode obtenerConductoresInOrderDesdeAPI() {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("park_id", proOpsProperties.getParkId());
            requestBody.put("car", new HashMap<>());
            requestBody.put("statuses", Arrays.asList("in_order", "free"));
            Map<String, Object> sort = new HashMap<>();
            sort.put("field", "status_duration");
            sort.put("direction", "desc");
            requestBody.put("sort", sort);

            ResponseEntity<String> response = ejecutarConRetryCookies(
                proOpsProperties.getYango().getDriverPointsUrl(), HttpMethod.POST, requestBody, this::crearHeadersDriversPointsConCookie);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readTree(response.getBody()).get("items");
            }
            return null;
        } catch (Exception e) {
            log.error("[FleetDriverService] error API drivers/points: {}", e.getMessage());
            return null;
        }
    }

    private List<String> extraerDriverIdsYBalances(JsonNode items, Map<String, Double> balanceMap) {
        List<String> driverIds = new ArrayList<>();
        for (JsonNode item : items) {
            String driverId = obtenerTexto(item, "driver_id");
            if (driverId == null || driverId.isEmpty()) continue;
            driverIds.add(driverId);
            if (item.has("balance") && !item.get("balance").isNull()) {
                balanceMap.put(driverId, item.get("balance").asDouble());
            }
        }
        return driverIds;
    }

    private PaginationResult aplicarPaginacion(
            List<String> driverIds, Map<String, Double> balanceMap, Integer page, Integer limit, int total) {
        int startIndex = page * limit;
        if (startIndex >= total) return null;
        int endIndex = Math.min(startIndex + limit, total);

        List<String> paginated = driverIds.subList(startIndex, endIndex);
        Map<String, Double> balanceMapPaginated = new HashMap<>();
        for (String id : paginated) {
            Double bal = balanceMap.get(id);
            if (bal != null) balanceMapPaginated.put(id, bal);
        }
        return new PaginationResult(paginated, balanceMapPaginated);
    }

    private DriversInOrderResponse obtenerDetallesConductores(List<String> driverIds, Map<String, Double> balanceMap) {
        long startTime = System.currentTimeMillis();
        try {
            FechaRango fechaRango = obtenerFechaRangoHoy();

            CompletableFuture<JsonNode> listFuture = CompletableFuture
                .supplyAsync(() -> obtenerItemsDesdeDriversList(driverIds), executorService)
                .exceptionally(ex -> { log.warn("[FleetDriverService] drivers/list falló: {}", ex.getMessage()); return null; });

            List<CompletableFuture<DriverDetails>> detailFutures = driverIds.stream()
                .map(driverId -> fetchDetailsForDriverAsync(driverId, fechaRango))
                .collect(Collectors.toList());

            CompletableFuture<?>[] all = new CompletableFuture<?>[1 + detailFutures.size()];
            all[0] = listFuture;
            for (int i = 0; i < detailFutures.size(); i++) all[i + 1] = detailFutures.get(i);
            CompletableFuture.allOf(all).get(DETAILS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            JsonNode items = listFuture.join();
            if (items == null || !items.isArray() || items.isEmpty()) {
                return DriversInOrderResponse.builder()
                    .conductores(crearConductoresBasicos(driverIds, balanceMap))
                    .total(driverIds.size())
                    .build();
            }

            Map<String, JsonNode> driverNodeById = indexarDriversPorId(items);
            Map<String, DriverDetails> detailsById = recolectarDetalles(driverIds, detailFutures);

            List<DriversInOrderResponse.DriverInOrderInfo> conductores =
                (driverIds.size() > PARALLEL_STREAM_THRESHOLD ? driverIds.parallelStream() : driverIds.stream())
                    .map(id -> construirDriverInOrderInfo(id, driverNodeById.get(id), detailsById.get(id), balanceMap.get(id)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.debug("[FleetDriverService] detallesConductores n={} ({}ms)", driverIds.size(), System.currentTimeMillis() - startTime);
            return DriversInOrderResponse.builder().conductores(conductores).total(conductores.size()).build();
        } catch (Exception e) {
            log.error("[FleetDriverService] error detallesConductores ({}ms): {}",
                System.currentTimeMillis() - startTime, e.getMessage(), e);
            return DriversInOrderResponse.builder().conductores(new ArrayList<>()).total(0).build();
        }
    }

    private Map<String, JsonNode> indexarDriversPorId(JsonNode items) {
        Map<String, JsonNode> driverNodeById = new HashMap<>();
        for (JsonNode item : items) {
            JsonNode driver = item.has("driver") ? item.get("driver") : item;
            String id = obtenerTexto(driver, "id");
            if (id != null && !id.isEmpty()) driverNodeById.put(id, driver);
        }
        return driverNodeById;
    }

    private Map<String, DriverDetails> recolectarDetalles(List<String> driverIds, List<CompletableFuture<DriverDetails>> futures) {
        Map<String, DriverDetails> detailsById = new HashMap<>();
        for (int i = 0; i < driverIds.size(); i++) {
            try {
                DriverDetails d = futures.get(i).join();
                if (d != null) detailsById.put(driverIds.get(i), d);
            } catch (Exception e) {
                log.warn("[FleetDriverService] detalle {} falló: {}", driverIds.get(i), e.getMessage());
            }
        }
        return detailsById;
    }

    private CompletableFuture<DriverDetails> fetchDetailsForDriverAsync(String driverId, FechaRango fechaRango) {
        CompletableFuture<GpsDataResult> gpsFuture = CompletableFuture
            .supplyAsync(() -> obtenerDatosGpsCompletos(driverId, fechaRango.dateFrom(), fechaRango.dateTo()), executorService)
            .exceptionally(ex -> null);
        CompletableFuture<CompletedOrdersResult> ordersFuture = CompletableFuture
            .supplyAsync(() -> obtenerOrdenesCompletadasDelDia(driverId), executorService)
            .exceptionally(ex -> CompletedOrdersResult.empty());
        CompletableFuture<List<DriversInOrderResponse.TripSimplified>> viajesFuture = CompletableFuture
            .supplyAsync(() -> obtenerViajesSimplificados(driverId, fechaRango.fechaHoyStr()), executorService)
            .exceptionally(ex -> new ArrayList<>());

        return CompletableFuture.allOf(gpsFuture, ordersFuture, viajesFuture)
            .thenApply(v -> new DriverDetails(gpsFuture.join(), ordersFuture.join(), viajesFuture.join()))
            .exceptionally(ex -> new DriverDetails(null, CompletedOrdersResult.empty(), new ArrayList<>()));
    }

    private DriversInOrderResponse.DriverInOrderInfo construirDriverInOrderInfo(
            String driverId, JsonNode driver, DriverDetails details, Double balanceFromPoints) {
        if (driver == null) return crearConductorBasico(driverId, balanceFromPoints);

        GpsDataResult gpsData = details != null ? details.gps() : null;
        CompletedOrdersResult orders = details != null ? details.orders() : CompletedOrdersResult.empty();
        List<DriversInOrderResponse.TripSimplified> viajes = details != null ? details.viajes() : new ArrayList<>();

        return DriversInOrderResponse.DriverInOrderInfo.builder()
            .id(driverId)
            .avatarUrl(obtenerTexto(driver, "avatar_url"))
            .balance(obtenerBalance(balanceFromPoints, driver))
            .firstName(obtenerTexto(driver, "first_name"))
            .lastName(obtenerTexto(driver, "last_name"))
            .status(obtenerTexto(driver, "status", "in_order"))
            .vehicleNumber(obtenerVehicleNumber(driver))
            .viajes(viajes)
            .summaryDistance(gpsData != null && gpsData.summaryDistance() != null
                ? gpsData.summaryDistance() : crearSummaryDistancePorDefecto())
            .totalActivityTime(gpsData != null && gpsData.totalActivityTime() != null ? gpsData.totalActivityTime() : 0L)
            .completedTripsCount(orders.count())
            .completedTripsTotalPrice(orders.totalPrice())
            .build();
    }

    private JsonNode obtenerItemsDesdeDriversList(List<String> driverIds) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("driver_ids", driverIds);
            requestBody.put("with_order_field", true);

            ResponseEntity<String> response = ejecutarConRetryCookies(
                proOpsProperties.getYango().getDriversListUrl(), HttpMethod.POST, requestBody, this::crearHeadersDriversPointsConCookie);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readTree(response.getBody()).get("items");
            }
            return null;
        } catch (Exception e) {
            log.error("[FleetDriverService] error drivers/list: {}", e.getMessage());
            return null;
        }
    }

    private FechaRango obtenerFechaRangoHoy() {
        LocalDate fechaHoy = LocalDate.now(LIMA_ZONE);
        String dateFrom = fechaHoy.atStartOfDay(LIMA_ZONE).toInstant().toString();
        String dateTo = fechaHoy.atTime(23, 59, 59).atZone(LIMA_ZONE).toInstant().toString();
        return new FechaRango(dateFrom, dateTo, fechaHoy.format(DATE_FORMATTER));
    }

    private String obtenerVehicleNumber(JsonNode driver) {
        if (driver.has("car_license_number") && !driver.get("car_license_number").isNull()) {
            return driver.get("car_license_number").asText();
        }
        if (driver.has("car_number") && !driver.get("car_number").isNull()) {
            return driver.get("car_number").asText();
        }
        return "";
    }

    @Override
    public String obtenerPlacaConductor(String driverId) {
        try {
            JsonNode items = obtenerItemsDesdeDriversList(List.of(driverId));
            if (items != null && items.isArray() && items.size() > 0) {
                return obtenerVehicleNumber(items.get(0));
            }
        } catch (Exception e) {
            log.warn("[FleetDriverService] error obteniendo placa driverId={}: {}", driverId, e.getMessage());
        }
        return "";
    }

    @Override
    public ContractorSuggestionsResponse getContractorSuggestions(String parkId, String telefono) {
        String telefonoNormalizado = normalizeTelefonoToPeruE164(telefono);
        String cacheKey = parkId + "|" + telefonoNormalizado;

        Cached<ContractorSuggestionsResponse> cached = contractorSuggestionsCache.get(cacheKey);
        if (cached != null && !cached.expired()) return cached.value();

        Map<String, Object> body = new HashMap<>();
        body.put("query", Map.of("text", telefonoNormalizado));

        try {
            ResponseEntity<String> response = ejecutarSuggestionsConConexionDirecta(body, parkId);
            ContractorSuggestionsResponse parsed;
            if (response == null || response.getBody() == null) {
                parsed = ContractorSuggestionsResponse.builder().suggestions(new ArrayList<>()).existing(false).build();
            } else {
                parsed = objectMapper.readValue(response.getBody(), ContractorSuggestionsResponse.class);
                if (parsed.getSuggestions() == null) parsed.setSuggestions(new ArrayList<>());
                parsed.setExisting(!parsed.getSuggestions().isEmpty());
            }
            contractorSuggestionsCache.put(cacheKey, Cached.of(parsed, CONTRACTOR_SUGGESTIONS_CACHE_TTL_MS));
            return parsed;
        } catch (Exception e) {
            log.error("[FleetDriverService] error suggestions parkId={}: {}", parkId, e.getMessage(), e);
            throw new RuntimeException("Error al obtener sugerencias de contratista: " + e.getMessage(), e);
        }
    }

    private ResponseEntity<String> ejecutarSuggestionsConConexionDirecta(Map<String, Object> body, String parkId) {
        for (int attempt = 0; attempt < 3; attempt++) {
            int cookieIndex = obtenerIndiceCookieValida();
            if (cookieIndex < 0) break;
            HttpHeaders headers = crearHeadersSuggestionsConCookieYParkId(obtenerCookiePorIndice(cookieIndex), parkId);
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    proOpsProperties.getYango().getSuggestionsUrl(), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
                int code = response.getStatusCode().value();
                if (response.getStatusCode().is2xxSuccessful()) return response;
                if (code == 401 || code == 403 || code == 429) {
                    marcarCookieInvalida(cookieIndex);
                    continue;
                }
                return response;
            } catch (HttpClientErrorException e) {
                int code = e.getStatusCode().value();
                if (code == 401 || code == 403 || code == 429) {
                    marcarCookieInvalida(cookieIndex);
                    continue;
                }
                throw e;
            } catch (Exception e) {
                if (attempt == 2) throw e;
                log.warn("[FleetDriverService] suggestions intento {}: {}", attempt + 1, e.getMessage());
            }
        }
        return null;
    }

    private static String normalizeTelefonoToPeruE164(String raw) {
        if (raw == null) return "+51";
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) return "+51";
        if (digits.length() == 9) return "+51" + digits;
        if (digits.length() == 10 && digits.startsWith("0")) return "+51" + digits.substring(1);
        if (digits.length() == 11 && digits.startsWith("51")) return "+" + digits;
        return "+" + digits;
    }

    private record PaginationResult(List<String> driverIds, Map<String, Double> balanceMap) {}

    private record DriversListSnapshot(List<String> driverIds, Map<String, Double> balanceMap) {}

    private record Cached<T>(T value, long expiresAt) {
        static <T> Cached<T> of(T value, long ttlMs) {
            return new Cached<>(value, System.currentTimeMillis() + ttlMs);
        }
        boolean expired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }

    private record GpsDataResult(DriversInOrderResponse.SummaryDistance summaryDistance, Long totalActivityTime) {}

    private record CompletedOrdersResult(Integer count, Double totalPrice) {
        static CompletedOrdersResult empty() {
            return new CompletedOrdersResult(0, 0.0);
        }
    }

    private record FechaRango(String dateFrom, String dateTo, String fechaHoyStr) {}

    private record DriverDetails(
            GpsDataResult gps,
            CompletedOrdersResult orders,
            List<DriversInOrderResponse.TripSimplified> viajes) {
        private DriverDetails {
            if (orders == null) orders = CompletedOrdersResult.empty();
            if (viajes == null) viajes = new ArrayList<>();
        }
    }

    private static String obtenerTexto(JsonNode node, String key) {
        if (node == null || key == null) return null;
        JsonNode value = node.get(key);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private static String obtenerTexto(JsonNode node, String key, String defaultValue) {
        String val = obtenerTexto(node, key);
        return val != null ? val : defaultValue;
    }

    private DriversInOrderResponse crearRespuestaVaciaConductores() {
        return DriversInOrderResponse.builder().conductores(new ArrayList<>()).total(0).build();
    }

    private List<DriversInOrderResponse.DriverInOrderInfo> crearConductoresBasicos(List<String> driverIds, Map<String, Double> balanceMap) {
        List<DriversInOrderResponse.DriverInOrderInfo> list = new ArrayList<>();
        for (String id : driverIds) {
            list.add(DriversInOrderResponse.DriverInOrderInfo.builder()
                    .id(id).firstName(id).lastName("").status("offline")
                    .balance(String.valueOf(balanceMap.getOrDefault(id, 0.0)))
                    .build());
        }
        return list;
    }

    private GpsDataResult obtenerDatosGpsCompletos(String driverId, String dateFrom, String dateTo) {
        return null;
    }

    private CompletedOrdersResult obtenerOrdenesCompletadasDelDia(String driverId) {
        return CompletedOrdersResult.empty();
    }

    private List<DriversInOrderResponse.TripSimplified> obtenerViajesSimplificados(String driverId, String fecha) {
        return new ArrayList<>();
    }

    private DriversInOrderResponse.DriverInOrderInfo crearConductorBasico(String driverId, Double balance) {
        return DriversInOrderResponse.DriverInOrderInfo.builder()
                .id(driverId).firstName(driverId).lastName("").status("offline")
                .balance(balance != null ? String.valueOf(balance) : "0.0")
                .build();
    }

    private String obtenerBalance(Double balanceFromPoints, JsonNode driver) {
        if (driver != null && driver.has("balance") && !driver.get("balance").isNull()) {
            return driver.get("balance").asText();
        }
        return balanceFromPoints != null ? String.valueOf(balanceFromPoints) : "0.0";
    }

    private DriversInOrderResponse.SummaryDistance crearSummaryDistancePorDefecto() {
        return DriversInOrderResponse.SummaryDistance.builder()
                .free(0.0).notActive(0.0).active(0.0)
                .build();
    }
}
