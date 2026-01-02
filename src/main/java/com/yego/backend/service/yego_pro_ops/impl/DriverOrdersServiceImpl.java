package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_pro_ops.api.request.DriverOrdersRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class DriverOrdersServiceImpl implements DriverOrdersService {
    
    private final RestTemplate restTemplate;
    private final RestTemplate yangoProxyRestTemplate;
    private final com.yego.backend.config.yego_pro_ops.ProxyConfig proxyConfig;
    private final DriverCloseService driverCloseService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong ultimaLlamadaTimestamp = new AtomicLong(0);
    
    @Autowired
    public DriverOrdersServiceImpl(
            RestTemplate restTemplate,
            @Qualifier("yangoProxyRestTemplate") org.springframework.web.client.RestTemplate yangoProxyRestTemplate,
            com.yego.backend.config.yego_pro_ops.ProxyConfig proxyConfig,
            DriverCloseService driverCloseService) {
        this.restTemplate = restTemplate;
        this.yangoProxyRestTemplate = yangoProxyRestTemplate;
        this.proxyConfig = proxyConfig;
        this.driverCloseService = driverCloseService;
    }
    
    private static final String YANGO_COOKIE_BASE = "i=x5tkbBS7C7HE+NXGcad3ZssQ3gf1F0rq356OWQvEx3ZB8N6sRw3Cgl6OxfwzvxG4EEjzwDu2xiGfC575M7+qz6ox3wc=; yandexuid=196877061764616562; yashr=2270601791764616562; yuidss=196877061764616562; ymex=2079976564.yrts.1764616564; receive-cookie-deprecation=1; gdpr=0; _ym_uid=1764616564116282218; _ym_d=1764616565; Session_id=3:1764616812.5.0.1764616812843:WbD9Jg:9933.1.2:1|2223153146.0.2.0:3.3:1764616812|60:11454337.136939.hHJxPhpQO1T97Iog_aHQCOuvpQo; sessar=1.1396519.CiCR_wLdjC3OTrDh2hgMr8--C-fwizMwlP9jW-dd6vGgRw.9KD2YMUjfA4ZbhzmsFVhHJOx2zEo94hMFlT83twWhyo; sessionid2=3:1764616812.5.0.1764616812843:WbD9Jg:9933.1.2:1|2223153146.0.2.0:3.3:1764616812|60:11454337.136939.fakesign0000000000000000000; yp=2079976812.udn.cDpnaW9tYXJvcnRlZ2E%3D; L=BBBBQ18BXmZ2XmtITlJ8VUBfcUBgeGFSPTgiLAs7KTUuNQkx.1764616812.1447419.396095.0920cd88815bbde83a0318732f9a8b82; yandex_login=giomarortega; _ym_isad=2; _yasc=GQ2XCBpQDzLhff5lrRlrxuaOh4LeuP1795j4xB9obQ+6KvYMs16SxDDeknIkX93UcXa/; bh=EjkiQ2hyb21pdW0iO3Y9IjE0MiIsICJCcmF2ZSI7dj0iMTQyIiwgIk5vdF9BIEJyYW5kIjt2PSI5OSIaA3g4NiIJMTQyLjAuMC4wKgI/MDoHIkxpbnV4IkIGNi4xNy40SgI2NFJJIkNocm9taXVtIjt2PSIxNDIuMC4wLjAiLCJCcmF2ZSI7dj0iMTQyLjAuMC4wIiwiTm90X0EgQnJhbmQiO3Y9Ijk5LjAuMC4wImCi1cLJBmoZ3MrpiA7yrLelC/v68OcN6//99g/4nMyHCA==";
    private static final String YANGO_ORDERS_API_URL = "https://fleet.yango.com/api/reports-api/v1/orders/list";
    private static final String PARK_ID = "64085dd85e124e2c808806f70d527ea8";
    private static final ZoneId ZONE_UTC_MINUS_5 = ZoneId.of("America/Lima");
    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter ORIGINAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
    
    /**
     * Obtiene el RestTemplate a usar (con proxy si está habilitado)
     */
    private RestTemplate getRestTemplate() {
        if (proxyConfig != null && proxyConfig.isEnabled() && yangoProxyRestTemplate != null) {
            return yangoProxyRestTemplate;
        }
        return restTemplate;
    }
    
    /**
     * Espera si es necesario para mantener un intervalo de 15 segundos entre llamadas consecutivas a la API de Yango
     * Aumentado de 10 a 15 segundos para evitar errores 429 (Too Many Requests) incluso con rotación de proxies
     */
    private void esperarSiEsNecesario() {
        long ahora = System.currentTimeMillis();
        long ultimaLlamada = ultimaLlamadaTimestamp.get();
        
        if (ultimaLlamada > 0) {
            long tiempoDesdeUltimaLlamada = ahora - ultimaLlamada;
            long tiempoAEsperar = 15000 - tiempoDesdeUltimaLlamada; // 15 segundos en lugar de 10
            
            if (tiempoAEsperar > 0) {
                log.debug("⏳ [DriverOrdersService] Esperando {} ms antes de la siguiente llamada a Yango API", tiempoAEsperar);
                try {
                    Thread.sleep(tiempoAEsperar);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("⚠️ [DriverOrdersService] Interrupción durante throttling", e);
                }
            }
        }
        
        ultimaLlamadaTimestamp.set(System.currentTimeMillis());
    }
    
    /**
     * Crea los headers HTTP necesarios para las llamadas a la API de Yango
     */
    private HttpHeaders crearHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", YANGO_COOKIE_BASE);
        headers.set("x-park-id", PARK_ID);
        headers.set("language", "es-419");
        headers.set("x-client-version", "fleet/19321");
        headers.set("origin", "https://fleet.yango.com");
        headers.set("accept-language", "es-419,es;q=0.9");
        return headers;
    }
    
    /**
     * Crea el request body para la API de Yango
     */
    private Map<String, Object> crearRequestBody(String driverId, String dateFrom, String dateTo, String cursor) {
        return crearRequestBody(driverId, dateFrom, dateTo, cursor, true);
    }
    
    /**
     * Crea el request body para la API de Yango
     * @param incluirFiltroCompletos Si es true, filtra solo por "complete". Si es false, no incluye el filtro de status.
     */
    private Map<String, Object> crearRequestBody(String driverId, String dateFrom, String dateTo, String cursor, boolean incluirFiltroCompletos) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("date_from", dateFrom);
        requestBody.put("date_to", dateTo);
        requestBody.put("date_type", "booked_at");
        requestBody.put("driver_id", driverId);
        if (incluirFiltroCompletos) {
            requestBody.put("order_statuses", java.util.Arrays.asList("complete"));
        }
        if (cursor != null && !cursor.isEmpty()) {
            requestBody.put("cursor", cursor);
        }
        return requestBody;
    }
    
    /**
     * Obtiene el rango de fechas del día actual en UTC-5
     */
    private String[] obtenerRangoFechaActual() {
        LocalDate hoy = LocalDate.now(ZONE_UTC_MINUS_5);
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia = hoy.atTime(23, 59, 59);
        
        ZonedDateTime dateFrom = inicioDia.atZone(ZONE_UTC_MINUS_5);
        ZonedDateTime dateTo = finDia.atZone(ZONE_UTC_MINUS_5);
        
        return new String[]{
            dateFrom.format(API_DATE_FORMATTER),
            dateTo.format(API_DATE_FORMATTER)
        };
    }
    
    @Override
    public DriverOrdersResponse obtenerOrdenesDelDia(DriverOrdersRequest request) {
        try {
            String driverId = request.getDriverId();
            log.info("📋 [DriverOrdersService] Obteniendo órdenes para driver_id: {}", driverId);
            
            String[] fechas = obtenerRangoFechaActual();
            String dateFromStr = fechas[0];
            String dateToStr = fechas[1];
            
            Map<String, Object> requestBody = crearRequestBody(driverId, dateFromStr, dateToStr, null);
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            
            esperarSiEsNecesario();
            
            HttpEntity<String> httpRequest = new HttpEntity<>(requestBodyJson, crearHeaders());
            
            ResponseEntity<String> response = getRestTemplate().exchange(
                YANGO_ORDERS_API_URL, 
                HttpMethod.POST, 
                httpRequest, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                DriverOrdersResponse resultado = procesarRespuesta(jsonResponse, dateFromStr, dateToStr);
                log.info("✅ [DriverOrdersService] Órdenes procesadas: {}", resultado.getOrders().size());
                return resultado;
            }
            
            log.warn("⚠️ [DriverOrdersService] Respuesta HTTP no exitosa. Status: {}", response.getStatusCode());
            return crearRespuestaVacia();
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("❌ [DriverOrdersService] Error HTTP al obtener órdenes: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return crearRespuestaVacia();
        } catch (Exception e) {
            log.error("❌ [DriverOrdersService] Error obteniendo órdenes: {}", e.getMessage(), e);
            return crearRespuestaVacia();
        }
    }
    
    private DriverOrdersResponse procesarRespuesta(JsonNode jsonResponse, String dateFrom, String dateTo) {
        List<OrderInfoResponse> orders = procesarViajesCompletos(jsonResponse);
        
        String cursor = null;
        boolean hasMore = false;
        if (jsonResponse.has("cursor") && !jsonResponse.get("cursor").isNull()) {
            cursor = jsonResponse.get("cursor").asText();
            hasMore = true;
        }
        
        return DriverOrdersResponse.builder()
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .orders(orders)
            .cursor(cursor)
            .hasMore(hasMore)
            .cierreRegistrado(false)
            .build();
    }
    
    /**
     * Convierte una hora de UTC+3 a UTC-5 (Perú)
     * Formato de entrada: "2025-12-02T17:19:08.607000+03:00"
     * Formato de salida: "2025-12-02T06:19:08"
     */
    private String convertirHoraAPeru(String fechaHoraOriginal) {
        if (fechaHoraOriginal == null || fechaHoraOriginal.isEmpty()) {
            return null;
        }
        
        try {
            ZonedDateTime fechaOriginal;
            
            // Intentar parsear con diferentes formatos
            try {
                // Intentar con el formato completo con microsegundos
                fechaOriginal = ZonedDateTime.parse(fechaHoraOriginal, ORIGINAL_DATE_FORMATTER);
            } catch (Exception e1) {
                try {
                    // Intentar parsear directamente (puede manejar varios formatos)
                    fechaOriginal = ZonedDateTime.parse(fechaHoraOriginal);
                } catch (Exception e2) {
                    // Si falla, intentar con formato ISO
                    fechaOriginal = java.time.Instant.parse(fechaHoraOriginal)
                        .atZone(ZoneId.of("UTC+3"));
                }
            }
            
            // Convertir a zona horaria de Perú (UTC-5)
            ZonedDateTime fechaPeru = fechaOriginal.withZoneSameInstant(ZONE_UTC_MINUS_5);
            
            // Formatear sin zona horaria para Perú
            return fechaPeru.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (Exception e) {
            log.warn("⚠️ [DriverOrdersService] Error convirtiendo hora: {} - {}", fechaHoraOriginal, e.getMessage());
            return null;
        }
    }
    
    private DriverOrdersResponse crearRespuestaVacia() {
        String[] fechas = obtenerRangoFechaActual();
        return crearRespuestaVaciaPaginada(fechas[0], fechas[1]);
    }
    
    @Override
    public DriverOrdersResponse obtenerViajesCompletos(String driverId, String dateFrom, String dateTo, String cursor) {
        try {
            log.info("📋 [DriverOrdersService] Obteniendo viajes completos para driver_id: {}", driverId);
            
            if (dateFrom == null || dateTo == null) {
                String[] fechas = obtenerRangoFechaActual();
                dateFrom = fechas[0];
                dateTo = fechas[1];
            }
            
            // Verificar si existe un cierre para esta fecha
            boolean tieneCierreRegistrado = verificarCierreRegistrado(driverId, dateFrom);
            
            // Siempre obtener los viajes (aunque haya un cierre registrado)
            
            List<OrderInfoResponse> todosLosViajes = new ArrayList<>();
            String cursorActual = cursor;
            
            // Paginación automática: cargar todas las páginas hasta que no haya más cursor
            // Solo incluir viajes COMPLETOS (filtrar por status = "complete")
            do {
                Map<String, Object> requestBody = crearRequestBody(driverId, dateFrom, dateTo, cursorActual, true);
                String requestBodyJson = objectMapper.writeValueAsString(requestBody);
                
                esperarSiEsNecesario();
                
                HttpEntity<String> httpRequest = new HttpEntity<>(requestBodyJson, crearHeaders());
                
                ResponseEntity<String> response = getRestTemplate().exchange(
                    YANGO_ORDERS_API_URL, 
                    HttpMethod.POST, 
                    httpRequest, 
                    String.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                    
                    // Procesar esta página - SOLO viajes completos (filtrar por status = "complete")
                    List<OrderInfoResponse> viajesPagina = procesarViajesCompletos(jsonResponse);
                    todosLosViajes.addAll(viajesPagina);
                    
                    // Verificar si hay más páginas
                    if (jsonResponse.has("cursor") && !jsonResponse.get("cursor").isNull()) {
                        cursorActual = jsonResponse.get("cursor").asText();
                        log.info("📄 [DriverOrdersService] Cargando siguiente página (cursor presente). Total acumulado: {}", 
                            todosLosViajes.size());
                    } else {
                        cursorActual = null; // No hay más páginas
                        log.info("✅ [DriverOrdersService] No hay más páginas. Total final: {}", todosLosViajes.size());
                    }
                } else {
                    log.warn("⚠️ [DriverOrdersService] Respuesta HTTP no exitosa. Status: {}", response.getStatusCode());
                    cursorActual = null; // Detener paginación
                }
                
            } while (cursorActual != null);
            
            return DriverOrdersResponse.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
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
    
    @Override
    public DriverOrdersResponse obtenerTodosLosViajes(String driverId, String dateFrom, String dateTo, String cursor) {
        try {
            log.info("📋 [DriverOrdersService] Obteniendo TODOS los viajes (sin filtro) para driver_id: {}", driverId);
            
            if (dateFrom == null || dateTo == null) {
                String[] fechas = obtenerRangoFechaActual();
                dateFrom = fechas[0];
                dateTo = fechas[1];
            }
            
            List<OrderInfoResponse> todosLosViajes = new ArrayList<>();
            String cursorActual = cursor;
            
            // Paginación automática: cargar todas las páginas hasta que no haya más cursor
            // Incluir TODOS los viajes (completos y cancelados) sin filtrar por status
            do {
                Map<String, Object> requestBody = crearRequestBody(driverId, dateFrom, dateTo, cursorActual, false);
                String requestBodyJson = objectMapper.writeValueAsString(requestBody);
                
                esperarSiEsNecesario();
                
                HttpEntity<String> httpRequest = new HttpEntity<>(requestBodyJson, crearHeaders());
                
                ResponseEntity<String> response = getRestTemplate().exchange(
                    YANGO_ORDERS_API_URL, 
                    HttpMethod.POST, 
                    httpRequest, 
                    String.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                    
                    // Procesar esta página - SIN filtrar por status, incluir todos los viajes (completos y cancelados)
                    List<OrderInfoResponse> viajesPagina = procesarTodosLosViajes(jsonResponse);
                    todosLosViajes.addAll(viajesPagina);
                    
                    // Verificar si hay más páginas
                    if (jsonResponse.has("cursor") && !jsonResponse.get("cursor").isNull()) {
                        cursorActual = jsonResponse.get("cursor").asText();
                        log.info("📄 [DriverOrdersService] Cargando siguiente página (cursor presente). Total acumulado: {}", 
                            todosLosViajes.size());
                    } else {
                        cursorActual = null; // No hay más páginas
                        log.info("✅ [DriverOrdersService] No hay más páginas. Total final: {}", todosLosViajes.size());
                    }
                } else {
                    log.warn("⚠️ [DriverOrdersService] Respuesta HTTP no exitosa. Status: {}", response.getStatusCode());
                    cursorActual = null; // Detener paginación
                }
                
            } while (cursorActual != null);
            
            return DriverOrdersResponse.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
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
    
    /**
     * Procesa la respuesta de la API y extrae TODOS los viajes de una página (sin filtrar por status)
     * Se usa para el cálculo de horas de turno donde se necesitan todos los viajes (completos y cancelados)
     */
    private List<OrderInfoResponse> procesarTodosLosViajes(JsonNode jsonResponse) {
        List<OrderInfoResponse> todosLosViajes = new ArrayList<>();
        JsonNode ordersNode = jsonResponse.get("orders");
        
        if (ordersNode != null && ordersNode.isArray()) {
            for (JsonNode orderNode : ordersNode) {
                try {
                    // NO filtrar por status, incluir todos los viajes
                    OrderInfoResponse viaje = mapearViajeCompleto(orderNode);
                    if (viaje != null) {
                        todosLosViajes.add(viaje);
                    }
                } catch (Exception e) {
                    log.error("❌ [DriverOrdersService] Error procesando viaje: {}", e.getMessage());
                }
            }
        }
        
        return todosLosViajes;
    }
    
    /**
     * Procesa la respuesta de la API y extrae solo los viajes completos de una página
     */
    private List<OrderInfoResponse> procesarViajesCompletos(JsonNode jsonResponse) {
        List<OrderInfoResponse> viajesCompletos = new ArrayList<>();
        JsonNode ordersNode = jsonResponse.get("orders");
        
        if (ordersNode != null && ordersNode.isArray()) {
            for (JsonNode orderNode : ordersNode) {
                try {
                    String status = orderNode.has("status") ? orderNode.get("status").asText() : null;
                    if (!"complete".equalsIgnoreCase(status)) {
                        continue;
                    }
                    
                    OrderInfoResponse viaje = mapearViajeCompleto(orderNode);
                    if (viaje != null) {
                        viajesCompletos.add(viaje);
                    }
                } catch (Exception e) {
                    log.error("❌ [DriverOrdersService] Error procesando viaje completo: {}", e.getMessage());
                }
            }
        }
        
        return viajesCompletos;
    }
    
    /**
     * Crea una respuesta vacía con paginación
     */
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
    
    /**
     * Crea una respuesta de error verificando si hay cierre registrado
     */
    private DriverOrdersResponse crearRespuestaError(String driverId, String dateFrom, String dateTo) {
        String[] fechas = dateFrom != null && dateTo != null ? 
            new String[]{dateFrom, dateTo} : obtenerRangoFechaActual();
        
        DriverOrdersResponse respuesta = crearRespuestaVaciaPaginada(fechas[0], fechas[1]);
        respuesta.setCierreRegistrado(verificarCierreRegistrado(driverId, dateFrom));
        return respuesta;
    }
    
    /**
     * Verifica si existe un cierre registrado para un driver y fecha específicos
     * Extrae la fecha del formato ISO (ej: "2025-12-14T00:00:00-05:00" -> "2025-12-14")
     */
    private boolean verificarCierreRegistrado(String driverId, String dateFrom) {
        if (dateFrom == null || dateFrom.isEmpty() || driverId == null || driverId.isEmpty()) {
            return false;
        }
        
        try {
            String fechaSolo = dateFrom.contains("T") ? dateFrom.split("T")[0] : dateFrom.substring(0, Math.min(10, dateFrom.length()));
            return driverCloseService.obtenerCierrePorDriverIdYFecha(driverId, fechaSolo).isPresent();
        } catch (Exception e) {
            log.warn("⚠️ [DriverOrdersService] Error verificando cierre: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extrae la distancia (mileage) de una orden y la convierte de metros a kilómetros
     */
    private Double extraerDistancia(JsonNode orderNode) {
        if (orderNode.has("mileage")) {
            return orderNode.get("mileage").asDouble() / 1000.0;
        }
        return null;
    }
    
    /**
     * Extrae un valor Double del JsonNode si existe, retorna null si no existe
     */
    private Double extraerDouble(JsonNode node, String key) {
        return node.has(key) ? node.get(key).asDouble() : null;
    }
    
    private OrderInfoResponse mapearViajeCompleto(JsonNode orderNode) {
        try {
            String bookedAtOriginal = orderNode.has("booked_at") ? orderNode.get("booked_at").asText() : null;
            String endedAtOriginal = orderNode.has("ended_at") ? orderNode.get("ended_at").asText() : null;
            
            String bookedAtPeru = convertirHoraAPeru(bookedAtOriginal);
            String endedAtPeru = convertirHoraAPeru(endedAtOriginal);
            
            Double distance = extraerDistancia(orderNode);
            
            return OrderInfoResponse.builder()
                .status(orderNode.has("status") ? orderNode.get("status").asText() : null)
                .shortId(orderNode.has("short_id") ? orderNode.get("short_id").asLong() : null)
                .id(orderNode.has("id") ? orderNode.get("id").asText() : null)
                .driverId(orderNode.has("driver_id") ? orderNode.get("driver_id").asText() : null)
                .driverFullName(orderNode.has("driver_full_name") ? orderNode.get("driver_full_name").asText() : null)
                .endedAt(endedAtPeru)
                .bookedAt(bookedAtPeru)
                .carBrandModel(orderNode.has("car_brand_model") ? orderNode.get("car_brand_model").asText() : null)
                .distance(distance)
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
                .addressFrom(orderNode.has("address_from") ? orderNode.get("address_from").asText() : null)
                .addressTo(orderNode.has("address_to") ? orderNode.get("address_to").asText() : null)
                .build();
        } catch (Exception e) {
            log.error("❌ [DriverOrdersService] Error mapeando viaje completo: {}", e.getMessage());
            return null;
        }
    }


    /**
     * Parsea una fecha en formato ISO a ZonedDateTime
     */
    private ZonedDateTime parsearFecha(String fechaStr) {
        try {
            // Intentar con formato original primero
            return ZonedDateTime.parse(fechaStr, ORIGINAL_DATE_FORMATTER);
        } catch (Exception e) {
            try {
                // Intentar con formato estándar
                return ZonedDateTime.parse(fechaStr, API_DATE_FORMATTER);
            } catch (Exception e2) {
                // Intentar parseo ISO estándar
                return ZonedDateTime.parse(fechaStr);
            }
        }
    }
}
