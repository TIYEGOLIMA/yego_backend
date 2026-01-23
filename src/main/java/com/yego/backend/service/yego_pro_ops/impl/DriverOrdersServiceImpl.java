package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yego.backend.entity.yego_pro_ops.api.request.DriverOrdersRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MultipleDriversTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;
import com.yego.backend.service.yego_pro_ops.DriverCloseService;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.yego.backend.config.yego_pro_ops.ProxyConfig;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.annotation.PreDestroy;

@Slf4j
@Service
public class DriverOrdersServiceImpl extends BaseYangoApiService implements DriverOrdersService {
    
    // ==================== CONSTANTS ====================
    
    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter ORIGINAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
    private static final DateTimeFormatter PERU_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos
    private static final long DEFAULT_THROTTLE_DELAY_MS = 15000; // 15 segundos
    private static final long SIMPLIFIED_TRIPS_THROTTLE_DELAY_MS = 3000; // 3 segundos
    private static final double METERS_TO_KM = 1000.0;
    private static final String STATUS_COMPLETE = "complete";
    private static final String DATE_TYPE_BOOKED_AT = "booked_at";
    
    // ==================== FIELDS ====================
    
    private final DriverCloseService driverCloseService;
    // Reducir threads para evitar saturar Nginx (768 worker_connections)
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final java.util.concurrent.ConcurrentHashMap<String, CacheEntry> viajesSimplificadosCache = 
        new java.util.concurrent.ConcurrentHashMap<>();
    
    // ==================== CONSTRUCTOR ====================
    
    @Autowired
    public DriverOrdersServiceImpl(
            RestTemplate restTemplate,
            @Qualifier("yangoProxyRestTemplate") RestTemplate yangoProxyRestTemplate,
            ProxyConfig proxyConfig,
            DriverCloseService driverCloseService) {
        super(restTemplate, yangoProxyRestTemplate, proxyConfig);
        this.driverCloseService = driverCloseService;
        log.info("✅ [DriverOrdersService] Inicializado correctamente - Cache TTL: {} minutos, ExecutorService: {} threads", 
            CACHE_TTL_MS / 60000, executorService.getClass().getSimpleName());
    }
    
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("⚠️ [DriverOrdersService] ExecutorService no terminó en 30 segundos, forzando cierre");
                executorService.shutdownNow();
            }
                } catch (InterruptedException e) {
            log.error("❌ [DriverOrdersService] Error cerrando ExecutorService", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // ==================== INTERNAL USE: CalculatedShiftService ====================
    
    @Override
    public DriverOrdersResponse obtenerOrdenesDelDia(DriverOrdersRequest request) {
        try {
            String driverId = request.getDriverId();
            log.info("📋 [DriverOrdersService] Obteniendo órdenes para driver_id: {}", driverId);
            
            String[] fechas = obtenerRangoFechaActual();
            Map<String, Object> requestBody = crearRequestBody(driverId, fechas[0], fechas[1], null, true);
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            
            esperarSiEsNecesario();
            
            ResponseEntity<String> response = ejecutarConRetryCookies(
                YANGO_ORDERS_API_URL, 
                HttpMethod.POST, 
                requestBodyJson,
                this::crearHeadersConCookie
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                DriverOrdersResponse resultado = procesarRespuesta(jsonResponse, fechas[0], fechas[1]);
                log.info("✅ [DriverOrdersService] Órdenes procesadas: {}", resultado.getOrders().size());
                return resultado;
            }
            
            log.warn("⚠️ [DriverOrdersService] Respuesta HTTP no exitosa. Status: {}", response.getStatusCode());
            return crearRespuestaVacia();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("❌ [DriverOrdersService] Error HTTP al obtener órdenes: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            return crearRespuestaVacia();
        } catch (Exception e) {
            log.error("❌ [DriverOrdersService] Error obteniendo órdenes: {}", e.getMessage(), e);
            return crearRespuestaVacia();
        }
    }
    
    // ==================== DETALLE VIEW ====================
    
    @Override
    public DriverOrdersResponse obtenerViajesCompletos(String driverId, String dateFrom, String dateTo, String cursor) {
        try {
            log.info("📋 [DriverOrdersService] Obteniendo viajes completos para driver_id: {}", driverId);
            
            FechaRango fechaRango = normalizarFechas(dateFrom, dateTo);
            boolean tieneCierreRegistrado = verificarCierreRegistrado(driverId, fechaRango.dateFrom);
            
            List<OrderInfoResponse> todosLosViajes = obtenerViajesConPaginacion(
                driverId, fechaRango.dateFrom, fechaRango.dateTo, cursor, true);
            
            return DriverOrdersResponse.builder()
                .dateFrom(fechaRango.dateFrom)
                .dateTo(fechaRango.dateTo)
                .orders(todosLosViajes)
                .cursor(null)
                .hasMore(false)
                .cierreRegistrado(tieneCierreRegistrado)
                .build();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("❌ [DriverOrdersService] Error HTTP al obtener viajes completos: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            return crearRespuestaError(driverId, dateFrom, dateTo);
        } catch (Exception e) {
            log.error("❌ [DriverOrdersService] Error obteniendo viajes completos: {}", e.getMessage(), e);
            return crearRespuestaError(driverId, dateFrom, dateTo);
        }
    }
    
    // ==================== INTERNAL USE: CalculatedShiftService ====================
    
    @Override
    public DriverOrdersResponse obtenerTodosLosViajes(String driverId, String dateFrom, String dateTo, String cursor) {
        try {
            log.info("📋 [DriverOrdersService] Obteniendo TODOS los viajes (sin filtro) para driver_id: {}", driverId);
            
            FechaRango fechaRango = normalizarFechas(dateFrom, dateTo);
            List<OrderInfoResponse> todosLosViajes = obtenerViajesConPaginacion(
                driverId, fechaRango.dateFrom, fechaRango.dateTo, cursor, false);
            
            return DriverOrdersResponse.builder()
                .dateFrom(fechaRango.dateFrom)
                .dateTo(fechaRango.dateTo)
                .orders(todosLosViajes)
                .cursor(null)
                .hasMore(false)
                .cierreRegistrado(false)
                .build();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("❌ [DriverOrdersService] Error HTTP al obtener todos los viajes: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            return crearRespuestaError(driverId, dateFrom, dateTo);
        } catch (Exception e) {
            log.error("❌ [DriverOrdersService] Error obteniendo todos los viajes: {}", e.getMessage(), e);
            return crearRespuestaError(driverId, dateFrom, dateTo);
        }
    }
    
    // ==================== INTERNAL USE: WebSocket ====================
    
    @Override
    public MultipleDriversTripsSimplifiedResponse obtenerViajesSimplificadosMultiples(
            List<String> driverIds, String dateFrom, String dateTo) {
        if (driverIds == null || driverIds.isEmpty()) {
            return MultipleDriversTripsSimplifiedResponse.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .drivers(new ArrayList<>())
                .build();
        }
        
        List<MultipleDriversTripsSimplifiedResponse.DriverTrips> drivers = driverIds.parallelStream()
            .map(driverId -> procesarViajesSimplificadosPorConductor(driverId, dateFrom, dateTo))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
        
        return MultipleDriversTripsSimplifiedResponse.builder()
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .drivers(drivers)
            .build();
    }
    
    // ==================== MONITOREO EN VIVO VIEW ====================
    
    @Override
    public DriverTripsSimplifiedResponse obtenerViajesSimplificadosPorFecha(String driverId, String fecha) {
        long startTime = System.currentTimeMillis();
        
        if (!validarParametros(driverId, fecha)) {
            return crearRespuestaVaciaViajesSimplificados();
        }
        
        String cacheKey = driverId + "_" + fecha;
        CacheEntry cached = viajesSimplificadosCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logCacheHit(startTime, cached);
            return cached.response;
        }
        
        try {
            FechaRango fechaRango = convertirFechaARango(fecha);
            DriverTripsSimplifiedResponse response = obtenerViajesSimplificados(
                driverId, fechaRango.dateFrom, fechaRango.dateTo);
            
            if (response != null && response.getTrips() != null) {
                viajesSimplificadosCache.put(cacheKey, new CacheEntry(response));
                log.debug("💾 [DriverOrdersService] Viajes simplificados guardados en caché para {} - {}", 
                    cacheKey, response.getTrips().size());
            }
            
            logTiempoTotal("obtenerViajesSimplificadosPorFecha", startTime, 
                response != null && response.getTrips() != null ? response.getTrips().size() : 0);
            
            return response;
        } catch (Exception e) {
            log.error("❌ [DriverOrdersService] Error obteniendo viajes simplificados por fecha para driver_id {} y fecha {} (después de {} ms): {}", 
                driverId, fecha, System.currentTimeMillis() - startTime, e.getMessage(), e);
            return crearRespuestaVaciaViajesSimplificados();
        }
    }
    
    // ==================== PRIVATE METHODS: REQUEST BUILDING ====================
    
    private Map<String, Object> crearRequestBody(String driverId, String dateFrom, String dateTo, String cursor) {
        return crearRequestBody(driverId, dateFrom, dateTo, cursor, true);
    }
    
    private Map<String, Object> crearRequestBody(String driverId, String dateFrom, String dateTo, 
                                                  String cursor, boolean incluirFiltroCompletos) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("date_from", dateFrom);
        requestBody.put("date_to", dateTo);
        requestBody.put("date_type", DATE_TYPE_BOOKED_AT);
        requestBody.put("driver_id", driverId);
        if (incluirFiltroCompletos) {
            requestBody.put("order_statuses", java.util.Arrays.asList(STATUS_COMPLETE));
        }
        if (cursor != null && !cursor.isEmpty()) {
            requestBody.put("cursor", cursor);
        }
        return requestBody;
    }
    
    // ==================== PRIVATE METHODS: DATE HANDLING ====================
    
    private String[] obtenerRangoFechaActual() {
        LocalDate hoy = LocalDate.now(LIMA_ZONE);
        ZonedDateTime dateFrom = hoy.atStartOfDay().atZone(LIMA_ZONE);
        ZonedDateTime dateTo = hoy.atTime(23, 59, 59).atZone(LIMA_ZONE);
        
        return new String[]{
            dateFrom.format(API_DATE_FORMATTER),
            dateTo.format(API_DATE_FORMATTER)
        };
    }
    
    private FechaRango normalizarFechas(String dateFrom, String dateTo) {
        if (dateFrom == null || dateTo == null) {
            String[] fechas = obtenerRangoFechaActual();
            return new FechaRango(fechas[0], fechas[1]);
        }
        return new FechaRango(dateFrom, dateTo);
    }
    
    private FechaRango convertirFechaARango(String fecha) {
        LocalDate fechaLocal = LocalDate.parse(fecha, DATE_ONLY_FORMATTER);
        String dateFrom = fechaLocal.atStartOfDay().atZone(LIMA_ZONE).format(ISO_FORMATTER);
        String dateTo = fechaLocal.atTime(23, 59, 59).atZone(LIMA_ZONE).format(ISO_FORMATTER);
        return new FechaRango(dateFrom, dateTo);
    }
    
    // ==================== PRIVATE METHODS: PAGINATION ====================
    
    private List<OrderInfoResponse> obtenerViajesConPaginacion(
            String driverId, String dateFrom, String dateTo, String cursor, boolean soloCompletos) {
        List<OrderInfoResponse> todosLosViajes = new ArrayList<>();
        String cursorActual = cursor;
        
        do {
            try {
                Map<String, Object> requestBody = crearRequestBody(driverId, dateFrom, dateTo, cursorActual, soloCompletos);
                String requestBodyJson = objectMapper.writeValueAsString(requestBody);
                
                esperarSiEsNecesario();
                
                // Usar ejecutarConRetryCookies para manejar automáticamente errores 401 con rotación de cookies
                ResponseEntity<String> response = ejecutarConRetryCookies(
                    YANGO_ORDERS_API_URL,
                    HttpMethod.POST,
                    requestBodyJson,
                    this::crearHeadersConCookie
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                    List<OrderInfoResponse> viajesPagina = soloCompletos 
                        ? procesarViajesCompletos(jsonResponse)
                        : procesarTodosLosViajes(jsonResponse);
                    todosLosViajes.addAll(viajesPagina);
                    
                    cursorActual = obtenerSiguienteCursor(jsonResponse, todosLosViajes.size());
                } else {
                    log.warn("⚠️ [DriverOrdersService] Respuesta HTTP no exitosa. Status: {}", response.getStatusCode());
                    cursorActual = null;
                    }
                } catch (Exception e) {
                log.error("❌ [DriverOrdersService] Error en paginación: {}", e.getMessage());
                cursorActual = null;
            }
        } while (cursorActual != null);
        
        return todosLosViajes;
    }
    
    private String obtenerSiguienteCursor(JsonNode jsonResponse, int totalAcumulado) {
        if (jsonResponse.has("cursor") && !jsonResponse.get("cursor").isNull()) {
            log.info("📄 [DriverOrdersService] Cargando siguiente página (cursor presente). Total acumulado: {}", 
                totalAcumulado);
            return jsonResponse.get("cursor").asText();
        } else {
            log.info("✅ [DriverOrdersService] No hay más páginas. Total final: {}", totalAcumulado);
            return null;
        }
    }
    
    // ==================== PRIVATE METHODS: RESPONSE PROCESSING ====================
    
    private DriverOrdersResponse procesarRespuesta(JsonNode jsonResponse, String dateFrom, String dateTo) {
        List<OrderInfoResponse> orders = procesarViajesCompletos(jsonResponse);
        String cursor = obtenerTexto(jsonResponse, "cursor");
        boolean hasMore = cursor != null;
        
        return DriverOrdersResponse.builder()
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .orders(orders)
            .cursor(cursor)
            .hasMore(hasMore)
            .cierreRegistrado(false)
            .build();
    }
    
    private List<OrderInfoResponse> procesarTodosLosViajes(JsonNode jsonResponse) {
        return procesarViajesDesdeJson(jsonResponse, false);
    }
    
    private List<OrderInfoResponse> procesarViajesCompletos(JsonNode jsonResponse) {
        return procesarViajesDesdeJson(jsonResponse, true);
    }
    
    private List<OrderInfoResponse> procesarViajesDesdeJson(JsonNode jsonResponse, boolean soloCompletos) {
        List<OrderInfoResponse> viajes = new ArrayList<>();
        JsonNode ordersNode = jsonResponse.get("orders");
        
        if (ordersNode != null && ordersNode.isArray()) {
            for (JsonNode orderNode : ordersNode) {
                try {
                    if (soloCompletos) {
                        String status = obtenerTexto(orderNode, "status");
                        if (!STATUS_COMPLETE.equalsIgnoreCase(status)) {
                        continue;
                        }
                    }
                    
                    OrderInfoResponse viaje = mapearViajeCompleto(orderNode);
                    if (viaje != null) {
                        viajes.add(viaje);
                    }
                } catch (Exception e) {
                    log.error("❌ [DriverOrdersService] Error procesando viaje: {}", e.getMessage());
                }
            }
        }
        
        return viajes;
    }
    
    // ==================== PRIVATE METHODS: ORDER MAPPING ====================
    
    private OrderInfoResponse mapearViajeCompleto(JsonNode orderNode) {
        try {
            return OrderInfoResponse.builder()
                .status(obtenerTexto(orderNode, "status"))
                .shortId(obtenerLong(orderNode, "short_id"))
                .id(obtenerTexto(orderNode, "id"))
                .endedAt(convertirHoraAPeru(obtenerTexto(orderNode, "ended_at")))
                .bookedAt(convertirHoraAPeru(obtenerTexto(orderNode, "booked_at")))
                .carBrandModel(obtenerTexto(orderNode, "car_brand_model"))
                .distance(extraerDistancia(orderNode))
                .cash(extraerDouble(orderNode, "price_cash"))
                .card(extraerDouble(orderNode, "price_card"))
                .price(extraerDouble(orderNode, "price"))
                .priceBonus(extraerDouble(orderNode, "price_bonus"))
                .priceCommissionPark(extraerDouble(orderNode, "price_commission_park"))
                .priceCommissionService(extraerDouble(orderNode, "price_commission_service"))
                .priceCorporate(extraerDouble(orderNode, "price_corporate"))
                .priceOther(extraerDouble(orderNode, "price_other"))
                .pricePartnerRides(extraerDouble(orderNode, "price_partner_rides"))
                .pricePromotion(extraerDouble(orderNode, "price_promotion"))
                .priceTip(extraerDouble(orderNode, "price_tip"))
                .addressFrom(obtenerTexto(orderNode, "address_from"))
                .addressTo(obtenerTexto(orderNode, "address_to"))
                .build();
        } catch (Exception e) {
            log.error("❌ [DriverOrdersService] Error mapeando viaje completo: {}", e.getMessage());
            return null;
        }
    }

    // ==================== PRIVATE METHODS: SIMPLIFIED TRIPS ====================
    
    private DriverTripsSimplifiedResponse obtenerViajesSimplificados(String driverId, String dateFrom, String dateTo) {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("⚡ [DriverOrdersService] Obteniendo viajes simplificados para driver_id: {}", driverId);
            
            Map<String, Object> requestBody = crearRequestBody(driverId, dateFrom, dateTo, null, true);
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            
            esperarSiEsNecesario(SIMPLIFIED_TRIPS_THROTTLE_DELAY_MS);
            
            ResponseEntity<String> response = ejecutarConRetryCookies(
                YANGO_ORDERS_API_URL,
                HttpMethod.POST,
                requestBodyJson,
                this::crearHeadersConCookie
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("⚠️ [DriverOrdersService] Respuesta HTTP no exitosa para viajes simplificados. Status: {}", 
                    response.getStatusCode());
                return crearRespuestaVaciaViajesSimplificados();
            }
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            List<OrderInfoResponse> orders = procesarViajesCompletos(jsonResponse);
            List<DriverTripsSimplifiedResponse.TripSimplified> tripsSimplified = convertirAViajesSimplificados(orders);
            
            logTiempoTotal("obtenerViajesSimplificados", startTime, tripsSimplified.size());
            
            return DriverTripsSimplifiedResponse.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .trips(tripsSimplified)
                .build();
        } catch (Exception e) {
            log.error("❌ [DriverOrdersService] Error obteniendo viajes simplificados (después de {} ms): {}", 
                System.currentTimeMillis() - startTime, e.getMessage(), e);
            return crearRespuestaVaciaViajesSimplificados();
        }
    }
    
    private List<DriverTripsSimplifiedResponse.TripSimplified> convertirAViajesSimplificados(
            List<OrderInfoResponse> orders) {
        return orders.stream()
            .map(order -> DriverTripsSimplifiedResponse.TripSimplified.builder()
                .status(order.getStatus())
                .shortId(order.getShortId())
                .id(order.getId())
                .endedAt(order.getEndedAt())
                .bookedAt(order.getBookedAt())
                .build())
            .collect(Collectors.toList());
    }
    
    private MultipleDriversTripsSimplifiedResponse.DriverTrips procesarViajesSimplificadosPorConductor(
            String driverId, String dateFrom, String dateTo) {
        
        if (driverId == null || driverId.isEmpty()) {
            log.warn("⚠️ [DriverOrdersService] driverId es null o vacío");
            return crearDriverTripsVacio(driverId);
        }
        
        try {
            DriverTripsSimplifiedResponse response = obtenerViajesSimplificados(driverId, dateFrom, dateTo);
            
            if (response == null || response.getTrips() == null || response.getTrips().isEmpty()) {
                log.debug("ℹ️ [DriverOrdersService] No hay viajes para driver_id: {}", driverId);
                return crearDriverTripsVacio(driverId);
            }
            
            List<MultipleDriversTripsSimplifiedResponse.TripSimplified> trips = response.getTrips().stream()
                .filter(trip -> trip != null)
                .map(trip -> MultipleDriversTripsSimplifiedResponse.TripSimplified.builder()
                    .status(trip.getStatus())
                    .id(trip.getId())
                    .endedAt(trip.getEndedAt())
                    .bookedAt(trip.getBookedAt())
                    .build())
                .collect(Collectors.toList());
            
            return MultipleDriversTripsSimplifiedResponse.DriverTrips.builder()
                .driverId(driverId)
                .driverFullName(null)
                .carBrandModel(null)
                .trips(trips)
                .build();
        } catch (Exception e) {
            log.error("❌ [DriverOrdersService] Error obteniendo viajes para driver_id {}: {}", driverId, e.getMessage(), e);
            return crearDriverTripsVacio(driverId);
        }
    }
    
    // ==================== PRIVATE METHODS: TIME CONVERSION ====================
    
    private String convertirHoraAPeru(String fechaHoraOriginal) {
        if (fechaHoraOriginal == null || fechaHoraOriginal.isEmpty()) {
            return null;
        }
        
        try {
            ZonedDateTime fechaOriginal = parsearFechaConMultipleFormatos(fechaHoraOriginal);
            ZonedDateTime fechaPeru = fechaOriginal.withZoneSameInstant(LIMA_ZONE);
            return fechaPeru.format(PERU_DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("⚠️ [DriverOrdersService] Error convirtiendo hora: {} - {}", fechaHoraOriginal, e.getMessage());
            return null;
        }
    }
    
    private ZonedDateTime parsearFechaConMultipleFormatos(String fechaStr) {
        try {
            return ZonedDateTime.parse(fechaStr, ORIGINAL_DATE_FORMATTER);
        } catch (Exception e1) {
            try {
                return ZonedDateTime.parse(fechaStr, API_DATE_FORMATTER);
            } catch (Exception e2) {
                try {
                return ZonedDateTime.parse(fechaStr);
                } catch (Exception e3) {
                    return java.time.Instant.parse(fechaStr).atZone(ZoneId.of("UTC+3"));
                }
            }
        }
    }
    
    // ==================== PRIVATE METHODS: VALIDATION ====================
    
    private boolean validarParametros(String driverId, String fecha) {
        if (driverId == null || driverId.isEmpty()) {
            log.warn("⚠️ [DriverOrdersService] driverId es null o vacío");
            return false;
        }
        if (fecha == null || fecha.isEmpty()) {
            log.warn("⚠️ [DriverOrdersService] fecha es null o vacía");
            return false;
        }
        return true;
    }
    
    // ==================== PRIVATE METHODS: CLOSE VERIFICATION ====================
    
    private boolean verificarCierreRegistrado(String driverId, String dateFrom) {
        if (dateFrom == null || dateFrom.isEmpty() || driverId == null || driverId.isEmpty()) {
            return false;
        }
        
        try {
            String fechaSolo = extraerFechaSolo(dateFrom);
            return driverCloseService.obtenerCierrePorDriverIdYFecha(driverId, fechaSolo).isPresent();
        } catch (Exception e) {
            log.warn("⚠️ [DriverOrdersService] Error verificando cierre: {}", e.getMessage());
            return false;
        }
    }
    
    private String extraerFechaSolo(String dateFrom) {
        if (dateFrom.contains("T")) {
            return dateFrom.split("T")[0];
        }
        return dateFrom.substring(0, Math.min(10, dateFrom.length()));
    }
    
    // ==================== PRIVATE METHODS: JSON HELPERS ====================
    
    private String obtenerTexto(JsonNode node, String key) {
        if (node != null && node.has(key) && !node.get(key).isNull()) {
            return node.get(key).asText();
        }
        return null;
    }
    
    private Long obtenerLong(JsonNode node, String key) {
        if (node != null && node.has(key) && !node.get(key).isNull()) {
            return node.get(key).asLong();
        }
        return null;
    }
    
    private Double extraerDouble(JsonNode node, String key) {
        if (node != null && node.has(key) && !node.get(key).isNull()) {
            return node.get(key).asDouble();
        }
        return null;
    }
    
    private Double extraerDistancia(JsonNode orderNode) {
        if (orderNode != null && orderNode.has("mileage")) {
            return orderNode.get("mileage").asDouble() / METERS_TO_KM;
        }
        return null;
    }
    
    // ==================== PRIVATE METHODS: RESPONSE BUILDERS ====================
    
    private DriverOrdersResponse crearRespuestaVacia() {
        String[] fechas = obtenerRangoFechaActual();
        return crearRespuestaVaciaPaginada(fechas[0], fechas[1]);
    }
    
    private DriverOrdersResponse crearRespuestaVaciaPaginada(String dateFrom, String dateTo) {
        return DriverOrdersResponse.builder()
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .orders(new ArrayList<>())
            .cursor(null)
            .hasMore(false)
            .cierreRegistrado(false)
            .build();
    }
    
    private DriverOrdersResponse crearRespuestaError(String driverId, String dateFrom, String dateTo) {
        FechaRango fechaRango = normalizarFechas(dateFrom, dateTo);
        DriverOrdersResponse respuesta = crearRespuestaVaciaPaginada(fechaRango.dateFrom, fechaRango.dateTo);
        respuesta.setCierreRegistrado(verificarCierreRegistrado(driverId, fechaRango.dateFrom));
        return respuesta;
    }
    
    private DriverTripsSimplifiedResponse crearRespuestaVaciaViajesSimplificados() {
        return DriverTripsSimplifiedResponse.builder()
            .dateFrom(null)
            .dateTo(null)
            .trips(new ArrayList<>())
            .build();
    }
    
    private MultipleDriversTripsSimplifiedResponse.DriverTrips crearDriverTripsVacio(String driverId) {
        return MultipleDriversTripsSimplifiedResponse.DriverTrips.builder()
            .driverId(driverId != null ? driverId : "")
            .driverFullName(null)
            .carBrandModel(null)
            .trips(new ArrayList<>())
            .build();
    }
    
    // ==================== PRIVATE METHODS: LOGGING HELPERS ====================
    
    private void logCacheHit(long startTime, CacheEntry cached) {
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("💾 [DriverOrdersService] Viajes simplificados obtenidos desde CACHÉ: {} ms ({:.2f} seg) - {} viajes", 
            totalTime, String.format("%.2f", totalTime / 1000.0), 
            cached.response.getTrips() != null ? cached.response.getTrips().size() : 0);
    }
    
    private void logTiempoTotal(String metodo, long startTime) {
        logTiempoTotal(metodo, startTime, null);
    }
    
    private void logTiempoTotal(String metodo, long startTime, Integer size) {
        long totalTime = System.currentTimeMillis() - startTime;
        if (size != null) {
            log.info("⏱️ [DriverOrdersService] TIEMPO TOTAL {}: {} ms ({:.2f} seg) - {} viajes", 
                metodo, totalTime, String.format("%.2f", totalTime / 1000.0), size);
        } else {
            log.info("⏱️ [DriverOrdersService] TIEMPO TOTAL {}: {} ms ({:.2f} seg)", 
                metodo, totalTime, String.format("%.2f", totalTime / 1000.0));
        }
    }
    
    // ==================== INNER CLASSES ====================
    
    private static class CacheEntry {
        final DriverTripsSimplifiedResponse response;
        final long timestamp;
        
        CacheEntry(DriverTripsSimplifiedResponse response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    private static class FechaRango {
        final String dateFrom;
        final String dateTo;
        
        FechaRango(String dateFrom, String dateTo) {
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
        }
    }
}
