package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yego.backend.entity.yego_pro_ops.api.request.DriverListRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.yego.backend.config.yego_pro_ops.ProxyConfig;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.annotation.PreDestroy;

@Slf4j
@Service
public class FleetDriverServiceImpl extends BaseYangoApiService implements FleetDriverService {
    
    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final int CACHE_THRESHOLD_MS = 100;
    private static final String EMPTY_STRING = "";
    
    private final com.yego.backend.service.yego_pro_ops.DriverOrdersService driverOrdersService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(20);
    
    public FleetDriverServiceImpl(
            RestTemplate restTemplate,
            @Qualifier("yangoProxyRestTemplate") RestTemplate yangoProxyRestTemplate,
            com.yego.backend.config.yego_pro_ops.ProxyConfig proxyConfig,
            com.yego.backend.service.yego_pro_ops.DriverOrdersService driverOrdersService) {
        super(restTemplate, yangoProxyRestTemplate, proxyConfig);
        this.driverOrdersService = driverOrdersService;
        log.info("✅ [FleetDriverService] Inicializado correctamente - ExecutorService: {} threads", 
            executorService.getClass().getSimpleName());
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
    
    // ==================== DETALLE VIEW ====================
    
    @Override
    public DriverListResponse obtenerListaConductores(List<String> workRuleIds) {
        try {
            DriverListRequest requestBody = crearDriverListRequest(workRuleIds);
            String requestJson = objectMapper.writeValueAsString(requestBody);

            ResponseEntity<String> response = ejecutarConRetryCookies(
                YANGO_CONTRACTORS_API_URL,
                HttpMethod.POST,
                requestJson,
                this::crearHeadersConCookie
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return transformarAListaConductores(jsonResponse);
            }
            
            return crearDriverListResponseVacio();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("❌ Error HTTP al obtener lista de conductores: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return crearDriverListResponseVacio();
        } catch (Exception e) {
            log.error("❌ Error obteniendo lista de conductores: {}", e.getMessage(), e);
            return crearDriverListResponseVacio();
        }
    }

    private DriverListRequest crearDriverListRequest(List<String> workRuleIds) {
            Map<String, Object> filter = new HashMap<>();
            if (workRuleIds != null && !workRuleIds.isEmpty()) {
                filter.put("work_rule_ids", workRuleIds);
            }
            
        return DriverListRequest.builder()
                .filter(filter)
                .limit(50)
                .projection(java.util.Arrays.asList(
                    "full_name", "avatar_url", "name", "status", "id", "phone",
                    "orders_count", "groups", "violations", "attestation_issues",
                    "balance", "balance_limit", "unblock_date", "photocheck_restrictions"
                ))
                .build();
    }

    private DriverListResponse transformarAListaConductores(JsonNode jsonResponse) {
        List<DriverListResponse.ContractorResponse> contractors = new ArrayList<>();
        JsonNode contractorsNode = jsonResponse.get("contractors");
        
        if (contractorsNode != null && contractorsNode.isArray()) {
            for (JsonNode contractorNode : contractorsNode) {
                try {
                    DriverListResponse.ContractorResponse contractor = mapearContractor(contractorNode);
                    if (contractor != null) {
                        contractors.add(contractor);
                    }
                } catch (Exception e) {
                    log.error("❌ Error mapeando contractor: {}", e.getMessage());
                }
            }
        }

        return DriverListResponse.builder().contractors(contractors).build();
    }

    private DriverListResponse.ContractorResponse mapearContractor(JsonNode contractorNode) {
        try {
            return DriverListResponse.ContractorResponse.builder()
                .id(obtenerTexto(contractorNode, "id"))
                .leadId(obtenerTexto(contractorNode, "lead_id"))
                .avatarUrl(obtenerTexto(contractorNode, "avatar_url"))
                .balance(obtenerTexto(contractorNode, "balance"))
                .balanceLimit(obtenerTexto(contractorNode, "balance_limit"))
                .fullName(obtenerTexto(contractorNode, "full_name"))
                .groups(obtenerListaTexto(contractorNode, "groups"))
                .hiringSegment(obtenerTexto(contractorNode, "hiring_segment"))
                .lastOrderDate(obtenerTexto(contractorNode, "last_order_date"))
                .lifecycleStep(obtenerTexto(contractorNode, "lifecycle_step"))
                .name(mapearNombre(contractorNode))
                .ordersCount(obtenerEntero(contractorNode, "orders_count"))
                .phone(obtenerTexto(contractorNode, "phone"))
                .status(obtenerTexto(contractorNode, "status"))
                .violations(obtenerListaTexto(contractorNode, "violations"))
                .attestationIssues(obtenerListaTexto(contractorNode, "attestation_issues"))
                .unblockDate(obtenerTexto(contractorNode, "unblock_date"))
                .photocheckRestrictions(obtenerListaTexto(contractorNode, "photocheck_restrictions"))
                .build();
        } catch (Exception e) {
            log.error("❌ Error mapeando contractor: {}", e.getMessage(), e);
            return null;
        }
    }

    private DriverListResponse.ContractorResponse.NameResponse mapearNombre(JsonNode contractorNode) {
        if (!contractorNode.has("name")) {
            return null;
        }
        JsonNode nameNode = contractorNode.get("name");
        return DriverListResponse.ContractorResponse.NameResponse.builder()
            .first(obtenerTexto(nameNode, "first"))
            .last(obtenerTexto(nameNode, "last"))
            .middle(obtenerTexto(nameNode, "middle"))
            .build();
    }

    // ==================== MONITOREO EN VIVO VIEW ====================
    
    @Override
    public DriversInOrderResponse obtenerConductoresEnOrden(Integer page, Integer limit) {
        long startTime = System.currentTimeMillis();
        try {
            JsonNode items = medirTiempo("Paso 1 - Obtener conductores desde API", 
                () -> obtenerConductoresInOrderDesdeAPI());
            
            if (items == null || !items.isArray() || items.isEmpty()) {
                return crearRespuestaVaciaConductores();
            }
            
            Map<String, Double> balanceMap = new HashMap<>();
            List<String> driverIdsInOrder = medirTiempo("Paso 2 - Extraer driverIds", 
                () -> extraerDriverIdsYBalances(items, balanceMap));
            
            if (driverIdsInOrder.isEmpty()) {
                return crearRespuestaVaciaConductores();
            }
            
            int totalConductores = driverIdsInOrder.size();
            PaginationResult pagination = medirTiempo("Paso 3 - Aplicar paginación", 
                () -> aplicarPaginacion(driverIdsInOrder, balanceMap, page, limit, totalConductores));
            
            if (pagination == null) {
                return DriversInOrderResponse.builder()
                    .conductores(new ArrayList<>())
                    .total(totalConductores)
                    .build();
            }
            
            DriversInOrderResponse result = medirTiempo("Paso 4 - Obtener detalles conductores", 
                () -> obtenerDetallesConductores(pagination.driverIds, pagination.balanceMap),
                pagination.driverIds.size());
            
            if (result == null || result.getConductores() == null || result.getConductores().isEmpty()) {
                result = DriversInOrderResponse.builder()
                    .conductores(crearConductoresBasicos(pagination.driverIds, pagination.balanceMap))
                    .total(totalConductores)
                .build();
            } else {
                result.setTotal(totalConductores);
            }
            
            logTiempoTotal("obtenerConductoresEnOrden", startTime);
            return result;
        } catch (Exception e) {
            log.error("❌ [FleetDriverService] Error obteniendo conductores en orden (después de {} ms): {}", 
                System.currentTimeMillis() - startTime, e.getMessage(), e);
            return crearRespuestaVaciaConductores();
        }
    }
    
    private JsonNode obtenerConductoresInOrderDesdeAPI() {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("park_id", PARK_ID);
            requestBody.put("car", new HashMap<>());
            requestBody.put("statuses", java.util.Arrays.asList("in_order", "free"));
            
            Map<String, Object> sort = new HashMap<>();
            sort.put("field", "status_duration");
            sort.put("direction", "desc");
            requestBody.put("sort", sort);
            
            ResponseEntity<String> response = ejecutarConRetryCookies(
                YANGO_API_URL, 
                HttpMethod.POST, 
                requestBody,
                this::crearHeadersDriversPointsConCookie
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return jsonResponse.get("items");
            }
            return null;
        } catch (Exception e) {
            log.error("❌ [FleetDriverService] Error obteniendo conductores desde API: {}", e.getMessage());
            return null;
        }
    }
    
    private List<String> extraerDriverIdsYBalances(JsonNode items, Map<String, Double> balanceMap) {
            List<String> driverIds = new ArrayList<>();
            for (JsonNode item : items) {
            String driverId = obtenerTexto(item, "driver_id");
                if (driverId != null && !driverId.isEmpty()) {
                    driverIds.add(driverId);
                if (item.has("balance") && !item.get("balance").isNull()) {
                    balanceMap.put(driverId, item.get("balance").asDouble());
                }
                }
            }
            return driverIds;
    }
    
    private PaginationResult aplicarPaginacion(List<String> driverIds, Map<String, Double> balanceMap, 
                                               Integer page, Integer limit, int total) {
        int startIndex = page * limit;
        int endIndex = Math.min(startIndex + limit, total);
        
        if (startIndex >= total) {
            return null;
        }
        
        List<String> driverIdsPaginated = driverIds.subList(startIndex, endIndex);
        Map<String, Double> balanceMapPaginated = new HashMap<>();
        for (String driverId : driverIdsPaginated) {
            if (balanceMap.containsKey(driverId)) {
                balanceMapPaginated.put(driverId, balanceMap.get(driverId));
            }
        }
        
        PaginationResult result = new PaginationResult();
        result.driverIds = driverIdsPaginated;
        result.balanceMap = balanceMapPaginated;
        return result;
    }
    
    private DriversInOrderResponse obtenerDetallesConductores(List<String> driverIds, Map<String, Double> balanceMap) {
        long startTime = System.currentTimeMillis();
        try {
            JsonNode items = obtenerItemsDesdeDriversList(driverIds);
            
            if (items == null || !items.isArray() || items.isEmpty()) {
                return DriversInOrderResponse.builder()
                    .conductores(crearConductoresBasicos(driverIds, balanceMap))
                    .total(driverIds.size())
                    .build();
            }
            
            FechaRango fechaRango = obtenerFechaRangoHoy();
            List<CompletableFuture<DriversInOrderResponse.DriverInOrderInfo>> futures = 
                crearFuturesParaConductores(items, balanceMap, fechaRango);
            
            List<DriversInOrderResponse.DriverInOrderInfo> conductores = ejecutarYRecolectarFutures(futures);
            
            logTiempoTotal("obtenerDetallesConductores", startTime, driverIds.size());
            return DriversInOrderResponse.builder()
                .conductores(conductores)
                .total(conductores.size())
                .build();
        } catch (Exception e) {
            log.error("❌ [FleetDriverService] Error en obtenerDetallesConductores (después de {} ms): {}", 
                System.currentTimeMillis() - startTime, e.getMessage(), e);
                return DriversInOrderResponse.builder()
                    .conductores(new ArrayList<>())
                    .total(0)
                    .build();
            }
    }
    
    private JsonNode obtenerItemsDesdeDriversList(List<String> driverIds) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("driver_ids", driverIds);
            requestBody.put("with_order_field", true);
            
            ResponseEntity<String> response = ejecutarConRetryCookies(
                YANGO_DRIVERS_LIST_API_URL,
                HttpMethod.POST,
                requestBody,
                this::crearHeadersDriversListConCookie
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return jsonResponse.get("items");
            }
            return null;
        } catch (Exception e) {
            log.error("❌ Error obteniendo items desde drivers/list: {}", e.getMessage());
            return null;
        }
    }
    
    private FechaRango obtenerFechaRangoHoy() {
        LocalDate fechaHoy = LocalDate.now(LIMA_ZONE);
        String dateFrom = fechaHoy.atStartOfDay(LIMA_ZONE).toInstant().toString();
        String dateTo = fechaHoy.atTime(23, 59, 59).atZone(LIMA_ZONE).toInstant().toString();
        String fechaHoyStr = fechaHoy.format(DATE_FORMATTER);
        return new FechaRango(dateFrom, dateTo, fechaHoyStr);
    }
    
    private List<CompletableFuture<DriversInOrderResponse.DriverInOrderInfo>> crearFuturesParaConductores(
            JsonNode items, Map<String, Double> balanceMap, FechaRango fechaRango) {
        List<CompletableFuture<DriversInOrderResponse.DriverInOrderInfo>> futures = new ArrayList<>();
            
            for (JsonNode item : items) {
                JsonNode driver = item.has("driver") ? item.get("driver") : item;
            String driverId = obtenerTexto(driver, "id");
            
            if (driverId == null || driverId.isEmpty()) {
                continue;
            }
            
            final String finalDriverId = driverId;
            final JsonNode finalDriver = driver;
            final Double balanceFromPoints = balanceMap.get(finalDriverId);
            final String vehicleNumber = obtenerVehicleNumber(finalDriver);
            
            CompletableFuture<DriversInOrderResponse.DriverInOrderInfo> future = CompletableFuture
                .supplyAsync(() -> procesarConductorCompleto(
                    finalDriverId, finalDriver, balanceFromPoints, vehicleNumber, fechaRango), 
                    executorService);
            
            futures.add(future);
        }
        
        return futures;
    }
    
    private String obtenerVehicleNumber(JsonNode driver) {
        if (driver.has("car_license_number") && !driver.get("car_license_number").isNull()) {
            return driver.get("car_license_number").asText();
        }
        if (driver.has("car_number") && !driver.get("car_number").isNull()) {
            return driver.get("car_number").asText();
        }
        return EMPTY_STRING;
    }
    
    private DriversInOrderResponse.DriverInOrderInfo procesarConductorCompleto(
            String driverId, JsonNode driver, Double balanceFromPoints, String vehicleNumber, FechaRango fechaRango) {
        
        CompletableFuture<GpsDataResult> gpsFuture = CompletableFuture
            .supplyAsync(() -> obtenerDatosGpsCompletos(driverId, fechaRango.dateFrom, fechaRango.dateTo), executorService);
        
        CompletableFuture<CompletedOrdersResult> ordersFuture = CompletableFuture
            .supplyAsync(() -> obtenerOrdenesCompletadasDelDia(driverId), executorService);
                
        CompletableFuture<List<DriversInOrderResponse.TripSimplified>> viajesFuture = CompletableFuture
            .supplyAsync(() -> obtenerViajesSimplificados(driverId, fechaRango.fechaHoyStr), executorService);
        
        GpsDataResult gpsData = obtenerResultado(gpsFuture, "GPS", driverId);
        CompletedOrdersResult completedOrders = obtenerResultado(ordersFuture, "órdenes", driverId, 
            () -> new CompletedOrdersResult(0, 0.0));
        List<DriversInOrderResponse.TripSimplified> viajes = obtenerResultado(viajesFuture, "viajes", driverId, 
            ArrayList::new);
        
        String balanceStr = obtenerBalance(balanceFromPoints, driver);
        DriversInOrderResponse.SummaryDistance summaryDistance = gpsData != null && gpsData.summaryDistance != null 
            ? gpsData.summaryDistance 
            : crearSummaryDistancePorDefecto();
        Long totalActivityTime = gpsData != null && gpsData.totalActivityTime != null ? gpsData.totalActivityTime : 0L;
        
        log.info("📊 [FleetDriverService] Conductor {}: {} viajes, total={}", 
            driverId, completedOrders.count, completedOrders.totalPrice);
        
        return DriversInOrderResponse.DriverInOrderInfo.builder()
                    .id(driverId)
            .avatarUrl(obtenerTexto(driver, "avatar_url"))
            .balance(balanceStr)
            .firstName(obtenerTexto(driver, "first_name"))
            .lastName(obtenerTexto(driver, "last_name"))
            .status(obtenerTexto(driver, "status", "in_order"))
            .vehicleNumber(vehicleNumber)
            .viajes(viajes)
            .summaryDistance(summaryDistance)
            .totalActivityTime(totalActivityTime)
                    .completedTripsCount(completedOrders.count)
                    .completedTripsTotalPrice(completedOrders.totalPrice)
                    .build();
    }
    
    private List<DriversInOrderResponse.TripSimplified> obtenerViajesSimplificados(String driverId, String fechaHoyStr) {
        long viajesStart = System.currentTimeMillis();
        try {
            com.yego.backend.entity.yego_pro_ops.api.response.DriverTripsSimplifiedResponse viajesResponse = 
                driverOrdersService.obtenerViajesSimplificadosPorFecha(driverId, fechaHoyStr);
            
            long viajesTime = System.currentTimeMillis() - viajesStart;
            boolean fromCache = viajesTime < CACHE_THRESHOLD_MS;
            log.info("⏱️ [FleetDriverService] Viajes simplificados para {}: {} ms {} ({} viajes)", 
                driverId, viajesTime, fromCache ? "(CACHÉ)" : "", 
                viajesResponse != null && viajesResponse.getTrips() != null ? viajesResponse.getTrips().size() : 0);
            
            if (viajesResponse != null && viajesResponse.getTrips() != null) {
                return viajesResponse.getTrips().stream()
                    .map(trip -> DriversInOrderResponse.TripSimplified.builder()
                        .status(trip.getStatus())
                        .shortId(trip.getShortId())
                        .id(trip.getId())
                        .endedAt(trip.getEndedAt())
                        .bookedAt(trip.getBookedAt())
                        .build())
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("⚠️ [FleetDriverService] Error obteniendo viajes para {} (después de {} ms): {}", 
                driverId, System.currentTimeMillis() - viajesStart, e.getMessage());
        }
        return new ArrayList<>();
    }
    
    private <T> T obtenerResultado(CompletableFuture<T> future, String tipo, String driverId) {
        return obtenerResultado(future, tipo, driverId, () -> null);
    }
    
    private <T> T obtenerResultado(CompletableFuture<T> future, String tipo, String driverId, java.util.function.Supplier<T> defaultValue) {
        try {
            return future.get();
        } catch (Exception e) {
            log.warn("⚠️ [FleetDriverService] Error obteniendo {} para {}: {}", tipo, driverId, e.getMessage());
            return defaultValue.get();
        }
    }
    
    private String obtenerBalance(Double balanceFromPoints, JsonNode driver) {
        if (balanceFromPoints != null) {
            return String.valueOf(balanceFromPoints);
        }
        return obtenerTexto(driver, "balance");
    }
    
    private List<DriversInOrderResponse.DriverInOrderInfo> ejecutarYRecolectarFutures(
            List<CompletableFuture<DriversInOrderResponse.DriverInOrderInfo>> futures) {
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            log.error("❌ Error ejecutando futures: {}", e.getMessage());
        }
        
        return futures.stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private GpsDataResult obtenerDatosGpsCompletos(String contractorProfileId, String dateFrom, String dateTo) {
        if (contractorProfileId == null || contractorProfileId.isEmpty()) {
            return null;
        }
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contractor_profile_id", contractorProfileId);
            requestBody.put("date_from", dateFrom);
            requestBody.put("date_to", dateTo);
            
            ResponseEntity<String> response = ejecutarConRetryCookies(
                YANGO_GPS_API_URL,
                HttpMethod.POST,
                requestBody,
                this::crearHeadersDriversListConCookie
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return mapearGpsDataResult(jsonResponse);
        } catch (Exception e) {
            return null;
        }
    }
    
    private GpsDataResult mapearGpsDataResult(JsonNode jsonResponse) {
        DriversInOrderResponse.SummaryDistance summaryDistance = mapearSummaryDistance(jsonResponse.get("summary_distance"));
        Long totalActivityTime = calcularTotalActivityTime(jsonResponse.get("detailed_gps"));
        
        GpsDataResult result = new GpsDataResult();
        result.summaryDistance = summaryDistance;
        result.totalActivityTime = totalActivityTime;
        return result;
    }
    
    private DriversInOrderResponse.SummaryDistance mapearSummaryDistance(JsonNode summaryDistanceNode) {
        if (summaryDistanceNode == null) {
            return null;
        }
        return DriversInOrderResponse.SummaryDistance.builder()
            .free(obtenerDoubleEnKm(summaryDistanceNode, "free"))
            .notActive(obtenerDoubleEnKm(summaryDistanceNode, "not_active"))
            .active(obtenerDoubleEnKm(summaryDistanceNode, "active"))
                    .build();
            }
            
    private double obtenerDoubleEnKm(JsonNode node, String key) {
        if (node.has(key) && !node.get(key).isNull()) {
            return node.get(key).asDouble() / 1000.0;
        }
        return 0.0;
    }
    
    private Long calcularTotalActivityTime(JsonNode detailedGpsNode) {
        if (detailedGpsNode == null || !detailedGpsNode.isArray()) {
            return 0L;
        }
        
                long totalTimeSeconds = 0;
                for (JsonNode trip : detailedGpsNode) {
            String driverStatus = obtenerTexto(trip, "driver_status");
                    if (("in_order".equals(driverStatus) || "free".equals(driverStatus)) 
                        && trip.has("status_time") && !trip.get("status_time").isNull()) {
                        totalTimeSeconds += trip.get("status_time").asLong();
                    }
                }
        return totalTimeSeconds;
        }
    
    private CompletedOrdersResult obtenerOrdenesCompletadasDelDia(String driverId) {
        CompletedOrdersResult result = new CompletedOrdersResult(0, 0.0);
        
        if (driverId == null || driverId.isEmpty()) {
            return result;
        }
        
        try {
            FechaRango fechaRango = obtenerFechaRangoIncome();
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("date_from", fechaRango.dateFrom);
            requestBodyMap.put("date_to", fechaRango.dateTo);
            requestBodyMap.put("driver_id", driverId);
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);
            
            ResponseEntity<String> response = ejecutarConRetryCookies(
                YANGO_DRIVER_INCOME_API_URL,
                HttpMethod.POST,
                requestBodyJson,
                this::crearHeadersConCookie
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("❌ [FleetDriverService] Error obteniendo income para {}: Status={}", 
                    driverId, response.getStatusCode());
                return result;
            }
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode ordersNode = jsonResponse.get("orders");
            
            if (ordersNode == null) {
                return result;
            }
            
            result.count = obtenerEntero(ordersNode, "count_completed", 0);
            result.totalPrice = obtenerDouble(ordersNode, "price", 0.0);
            
            log.info("✅ [FleetDriverService] Órdenes completadas para {}: count={}, totalPrice={}", 
                driverId, result.count, result.totalPrice);
            
            return result;
        } catch (Exception e) {
            log.error("❌ [FleetDriverService] Excepción obteniendo income para {}: {}", driverId, e.getMessage(), e);
            return result;
        }
    }
    
    private FechaRango obtenerFechaRangoIncome() {
        LocalDate fechaHoy = LocalDate.now(LIMA_ZONE);
        String dateFrom = fechaHoy.atStartOfDay().atZone(LIMA_ZONE).format(DATETIME_FORMATTER);
        String dateTo = fechaHoy.atTime(23, 59, 59).atZone(LIMA_ZONE).format(DATETIME_FORMATTER);
        return new FechaRango(dateFrom, dateTo, null);
    }
    
    // ==================== HELPERS ====================
    
    private DriversInOrderResponse.SummaryDistance crearSummaryDistancePorDefecto() {
        return DriversInOrderResponse.SummaryDistance.builder()
            .free(0.0)
            .notActive(0.0)
            .active(0.0)
            .build();
    }
    
    private DriversInOrderResponse.DriverInOrderInfo crearConductorBasico(String driverId, Double balance) {
        return DriversInOrderResponse.DriverInOrderInfo.builder()
            .id(driverId)
            .balance(balance != null ? String.valueOf(balance) : "0.0")
            .status("in_order")
            .vehicleNumber(EMPTY_STRING)
            .viajes(new ArrayList<>())
            .summaryDistance(crearSummaryDistancePorDefecto())
            .totalActivityTime(0L)
            .completedTripsCount(0)
            .completedTripsTotalPrice(0.0)
            .build();
            }
            
    private List<DriversInOrderResponse.DriverInOrderInfo> crearConductoresBasicos(List<String> driverIds, Map<String, Double> balanceMap) {
        return driverIds.stream()
            .map(driverId -> crearConductorBasico(driverId, balanceMap.get(driverId)))
            .collect(Collectors.toList());
    }
    
    private DriversInOrderResponse crearRespuestaVaciaConductores() {
        return DriversInOrderResponse.builder()
            .conductores(new ArrayList<>())
            .total(0)
            .build();
    }
    
    private DriverListResponse crearDriverListResponseVacio() {
        return DriverListResponse.builder().contractors(new ArrayList<>()).build();
                }
                
    
    // ==================== JSON HELPERS ====================
    
    private String obtenerTexto(JsonNode node, String key) {
        return obtenerTexto(node, key, null);
    }
    
    private String obtenerTexto(JsonNode node, String key, String defaultValue) {
        if (node != null && node.has(key) && !node.get(key).isNull()) {
            return node.get(key).asText();
        }
        return defaultValue;
    }
    
    private Integer obtenerEntero(JsonNode node, String key) {
        return obtenerEntero(node, key, null);
    }
    
    private Integer obtenerEntero(JsonNode node, String key, Integer defaultValue) {
        if (node != null && node.has(key) && !node.get(key).isNull()) {
            return node.get(key).asInt();
        }
        return defaultValue;
    }
    
    private Double obtenerDouble(JsonNode node, String key, Double defaultValue) {
        if (node != null && node.has(key) && !node.get(key).isNull()) {
            return node.get(key).asDouble();
        }
        return defaultValue;
    }
    
    private List<String> obtenerListaTexto(JsonNode node, String key) {
        List<String> lista = new ArrayList<>();
        if (node != null && node.has(key) && node.get(key).isArray()) {
            for (JsonNode item : node.get(key)) {
                lista.add(item.asText());
            }
        }
        return lista;
    }
    
    // ==================== LOGGING HELPERS ====================
    
    private <T> T medirTiempo(String paso, java.util.function.Supplier<T> supplier) {
        return medirTiempo(paso, supplier, null);
                    }
    
    private <T> T medirTiempo(String paso, java.util.function.Supplier<T> supplier, Integer size) {
        long start = System.currentTimeMillis();
        T result = supplier.get();
        long time = System.currentTimeMillis() - start;
        if (size != null) {
            log.info("⏱️ [FleetDriverService] {}: {} ms ({} elementos)", paso, time, size);
        } else {
            log.info("⏱️ [FleetDriverService] {}: {} ms", paso, time);
        }
        return result;
    }
    
    private void logTiempoTotal(String metodo, long startTime) {
        logTiempoTotal(metodo, startTime, null);
    }
    
    private void logTiempoTotal(String metodo, long startTime, Integer size) {
        long totalTime = System.currentTimeMillis() - startTime;
        if (size != null) {
            log.info("⏱️ [FleetDriverService] TIEMPO TOTAL {}: {} ms ({:.2f} seg) para {} elementos", 
                metodo, totalTime, String.format("%.2f", totalTime / 1000.0), size);
        } else {
            log.info("⏱️ [FleetDriverService] TIEMPO TOTAL {}: {} ms ({:.2f} seg)", 
                metodo, totalTime, String.format("%.2f", totalTime / 1000.0));
        }
    }
    
    // ==================== INNER CLASSES ====================
    
    private static class PaginationResult {
        List<String> driverIds;
        Map<String, Double> balanceMap;
    }
    
    private static class GpsDataResult {
        DriversInOrderResponse.SummaryDistance summaryDistance;
        Long totalActivityTime;
    }
    
    private static class CompletedOrdersResult {
        Integer count;
        Double totalPrice;
        
        CompletedOrdersResult(Integer count, Double totalPrice) {
            this.count = count;
            this.totalPrice = totalPrice;
        }
    }
    
    private static class FechaRango {
        final String dateFrom;
        final String dateTo;
        final String fechaHoyStr;
        
        FechaRango(String dateFrom, String dateTo, String fechaHoyStr) {
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.fechaHoyStr = fechaHoyStr;
    }
}
}
