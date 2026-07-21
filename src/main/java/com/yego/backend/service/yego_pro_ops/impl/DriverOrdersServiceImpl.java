package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.config.yego_pro_ops.ProxyConfig;
import com.yego.backend.config.yego_pro_ops.YegoProOpsProperties;
import com.yego.backend.integration.YangoCookiePool;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MultipleDriversTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;
import com.yego.backend.service.yego_pro_ops.DriverCloseService;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DriverOrdersServiceImpl extends BaseYangoApiService implements DriverOrdersService {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter ORIGINAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
    private static final DateTimeFormatter PERU_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final long CACHE_TTL_PAST_DATE_MS = 60 * 60 * 1000L;
    private static final double METERS_TO_KM = 1000.0;
    private static final String STATUS_COMPLETE = "complete";
    private static final String DATE_TYPE_BOOKED_AT = "booked_at";

    private final DriverCloseService driverCloseService;
    private final ConcurrentHashMap<String, CacheEntry> viajesSimplificadosCache = new ConcurrentHashMap<>();

    public DriverOrdersServiceImpl(
            RestTemplate restTemplate,
            @Qualifier("yangoProxyRestTemplate") RestTemplate yangoProxyRestTemplate,
            ProxyConfig proxyConfig,
            YangoCookiePool cookiePool,
            ObjectMapper objectMapper,
            YegoProOpsProperties proOpsProperties,
            DriverCloseService driverCloseService) {
        super(restTemplate, yangoProxyRestTemplate, proxyConfig, cookiePool, objectMapper, proOpsProperties);
        this.driverCloseService = driverCloseService;
    }

    @Override
    public DriverOrdersResponse obtenerViajesCompletos(String driverId, String dateFrom, String dateTo) {
        try {
            FechaRango fechaRango = normalizarFechas(dateFrom, dateTo);
            boolean tieneCierre = verificarCierreRegistrado(driverId, fechaRango.dateFrom());
            List<OrderInfoResponse> viajes = obtenerViajesConPaginacion(
                driverId, fechaRango.dateFrom(), fechaRango.dateTo());

            return DriverOrdersResponse.builder()
                .dateFrom(fechaRango.dateFrom())
                .dateTo(fechaRango.dateTo())
                .orders(viajes)
                .cierreRegistrado(tieneCierre)
                .build();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[DriverOrdersService] HTTP {} viajes completos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return crearRespuestaError(driverId, dateFrom, dateTo);
        } catch (Exception e) {
            log.error("[DriverOrdersService] error viajes completos: {}", e.getMessage(), e);
            return crearRespuestaError(driverId, dateFrom, dateTo);
        }
    }

    @Override
    public MultipleDriversTripsSimplifiedResponse obtenerViajesSimplificadosMultiples(
            List<String> driverIds, String dateFrom, String dateTo) {
        if (driverIds == null || driverIds.isEmpty()) {
            return MultipleDriversTripsSimplifiedResponse.builder()
                .dateFrom(dateFrom).dateTo(dateTo).drivers(new ArrayList<>()).build();
        }

        List<MultipleDriversTripsSimplifiedResponse.DriverTrips> drivers = driverIds.parallelStream()
            .map(driverId -> procesarViajesSimplificadosPorConductor(driverId, dateFrom, dateTo))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return MultipleDriversTripsSimplifiedResponse.builder()
            .dateFrom(dateFrom).dateTo(dateTo).drivers(drivers).build();
    }

    @Override
    public DriverTripsSimplifiedResponse obtenerViajesSimplificadosPorFecha(String driverId, String fecha) {
        if (!validarParametros(driverId, fecha)) {
            return crearRespuestaVaciaViajesSimplificados();
        }
        String cacheKey = driverId + "_" + fecha;
        CacheEntry cached = viajesSimplificadosCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.response();
        }
        try {
            FechaRango fechaRango = convertirFechaARango(fecha);
            LocalDate fechaLocal = LocalDate.parse(fecha, DATE_ONLY_FORMATTER);
            boolean esFechaPasada = fechaLocal.isBefore(LocalDate.now(LIMA_ZONE));

            DriverTripsSimplifiedResponse response = obtenerViajesSimplificados(
                driverId, fechaRango.dateFrom(), fechaRango.dateTo());

            if (response != null && response.getTrips() != null) {
                long ttl = esFechaPasada ? CACHE_TTL_PAST_DATE_MS : CACHE_TTL_MS;
                viajesSimplificadosCache.put(cacheKey, CacheEntry.of(response, ttl));
            }
            return response != null ? response : crearRespuestaVaciaViajesSimplificados();
        } catch (Exception e) {
            log.error("[DriverOrdersService] error viajes simplificados driverId={} fecha={}: {}",
                driverId, fecha, e.getMessage(), e);
            return crearRespuestaVaciaViajesSimplificados();
        }
    }

    private Map<String, Object> crearRequestBody(String driverId, String dateFrom, String dateTo, String cursor) {
        Map<String, Object> body = new HashMap<>();
        body.put("date_from", dateFrom);
        body.put("date_to", dateTo);
        body.put("date_type", DATE_TYPE_BOOKED_AT);
        body.put("driver_id", driverId);
        body.put("order_statuses", List.of(STATUS_COMPLETE));
        if (cursor != null && !cursor.isEmpty()) body.put("cursor", cursor);
        return body;
    }

    private FechaRango obtenerRangoFechaActual() {
        LocalDate hoy = LocalDate.now(LIMA_ZONE);
        return new FechaRango(
            hoy.atStartOfDay().atZone(LIMA_ZONE).format(API_DATE_FORMATTER),
            hoy.atTime(23, 59, 59).atZone(LIMA_ZONE).format(API_DATE_FORMATTER));
    }

    private FechaRango normalizarFechas(String dateFrom, String dateTo) {
        if (dateFrom == null || dateTo == null) return obtenerRangoFechaActual();
        return new FechaRango(dateFrom, dateTo);
    }

    private FechaRango convertirFechaARango(String fecha) {
        LocalDate fechaLocal = LocalDate.parse(fecha, DATE_ONLY_FORMATTER);
        return new FechaRango(
            fechaLocal.atStartOfDay().atZone(LIMA_ZONE).format(API_DATE_FORMATTER),
            fechaLocal.atTime(23, 59, 59).atZone(LIMA_ZONE).format(API_DATE_FORMATTER));
    }

    private List<OrderInfoResponse> obtenerViajesConPaginacion(
            String driverId, String dateFrom, String dateTo) {
        List<OrderInfoResponse> todos = new ArrayList<>();
        String cursorActual = null;

        do {
            try {
                Map<String, Object> body = crearRequestBody(driverId, dateFrom, dateTo, cursorActual);
                String bodyJson = objectMapper.writeValueAsString(body);

                ResponseEntity<String> response = ejecutarConRetryCookies(
                    proOpsProperties.getYango().getOrdersUrl(), HttpMethod.POST, bodyJson, this::crearHeadersConCookie);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                    todos.addAll(procesarViajesCompletos(jsonResponse));
                    cursorActual = obtenerSiguienteCursor(jsonResponse);
                } else {
                    cursorActual = null;
                }
            } catch (Exception e) {
                log.error("[DriverOrdersService] error paginación: {}", e.getMessage());
                cursorActual = null;
            }
        } while (cursorActual != null);

        return todos;
    }

    private String obtenerSiguienteCursor(JsonNode jsonResponse) {
        return jsonResponse.has("cursor") && !jsonResponse.get("cursor").isNull()
            ? jsonResponse.get("cursor").asText()
            : null;
    }

    private List<OrderInfoResponse> procesarViajesCompletos(JsonNode jsonResponse) {
        JsonNode ordersNode = jsonResponse.get("orders");
        if (ordersNode == null || !ordersNode.isArray()) return new ArrayList<>();

        List<OrderInfoResponse> viajes = new ArrayList<>(ordersNode.size());
        for (JsonNode orderNode : ordersNode) {
            try {
                String status = obtenerTexto(orderNode, "status");
                if (!STATUS_COMPLETE.equalsIgnoreCase(status)) continue;
                OrderInfoResponse viaje = mapearViajeCompleto(orderNode);
                if (viaje != null) viajes.add(viaje);
            } catch (Exception e) {
                log.error("[DriverOrdersService] error procesando viaje: {}", e.getMessage());
            }
        }
        return viajes;
    }

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
                .pricePromotion(extraerDouble(orderNode, "price_promotion"))
                .priceTip(extraerDouble(orderNode, "price_tip"))
                .carLicenseNumber(obtenerTexto(orderNode, "car_license_number"))
                .addressFrom(obtenerTexto(orderNode, "address_from"))
                .addressTo(obtenerTexto(orderNode, "address_to"))
                .build();
        } catch (Exception e) {
            log.error("[DriverOrdersService] error mapeando viaje completo: {}", e.getMessage());
            return null;
        }
    }

    private DriverTripsSimplifiedResponse obtenerViajesSimplificados(
            String driverId, String dateFrom, String dateTo) {
        try {
            Map<String, Object> body = crearRequestBody(driverId, dateFrom, dateTo, null);
            String bodyJson = objectMapper.writeValueAsString(body);

            ResponseEntity<String> response = ejecutarConRetryCookies(
                proOpsProperties.getYango().getOrdersUrl(), HttpMethod.POST, bodyJson, this::crearHeadersConCookie);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return crearRespuestaVaciaViajesSimplificados();
            }
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return DriverTripsSimplifiedResponse.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .trips(extraerViajesSimplificadosDesdeJson(jsonResponse))
                .build();
        } catch (Exception e) {
            log.error("[DriverOrdersService] error viajes simplificados: {}", e.getMessage(), e);
            return crearRespuestaVaciaViajesSimplificados();
        }
    }

    private List<DriverTripsSimplifiedResponse.TripSimplified> extraerViajesSimplificadosDesdeJson(JsonNode jsonResponse) {
        JsonNode ordersNode = jsonResponse.get("orders");
        if (ordersNode == null || !ordersNode.isArray()) return new ArrayList<>(0);

        List<DriverTripsSimplifiedResponse.TripSimplified> out = new ArrayList<>(ordersNode.size());
        for (JsonNode orderNode : ordersNode) {
            String status = obtenerTexto(orderNode, "status");
            if (!STATUS_COMPLETE.equalsIgnoreCase(status)) continue;
            out.add(DriverTripsSimplifiedResponse.TripSimplified.builder()
                .status(status)
                .shortId(obtenerLong(orderNode, "short_id"))
                .id(obtenerTexto(orderNode, "id"))
                .endedAt(convertirHoraAPeru(obtenerTexto(orderNode, "ended_at")))
                .bookedAt(convertirHoraAPeru(obtenerTexto(orderNode, "booked_at")))
                .build());
        }
        return out;
    }

    private MultipleDriversTripsSimplifiedResponse.DriverTrips procesarViajesSimplificadosPorConductor(
            String driverId, String dateFrom, String dateTo) {
        if (driverId == null || driverId.isEmpty()) return crearDriverTripsVacio(driverId);
        try {
            DriverTripsSimplifiedResponse response = obtenerViajesSimplificados(driverId, dateFrom, dateTo);
            if (response == null || response.getTrips() == null || response.getTrips().isEmpty()) {
                return crearDriverTripsVacio(driverId);
            }
            List<MultipleDriversTripsSimplifiedResponse.TripSimplified> trips = response.getTrips().stream()
                .filter(Objects::nonNull)
                .map(t -> MultipleDriversTripsSimplifiedResponse.TripSimplified.builder()
                    .status(t.getStatus())
                    .id(t.getId())
                    .endedAt(t.getEndedAt())
                    .bookedAt(t.getBookedAt())
                    .build())
                .collect(Collectors.toList());

            return MultipleDriversTripsSimplifiedResponse.DriverTrips.builder()
                .driverId(driverId)
                .trips(trips)
                .build();
        } catch (Exception e) {
            log.error("[DriverOrdersService] error viajes driverId={}: {}", driverId, e.getMessage(), e);
            return crearDriverTripsVacio(driverId);
        }
    }

    private String convertirHoraAPeru(String fechaHoraOriginal) {
        if (fechaHoraOriginal == null || fechaHoraOriginal.isEmpty()) return null;
        try {
            return parsearFechaConMultipleFormatos(fechaHoraOriginal)
                .withZoneSameInstant(LIMA_ZONE)
                .format(PERU_DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("[DriverOrdersService] error convirtiendo hora: {} - {}", fechaHoraOriginal, e.getMessage());
            return null;
        }
    }

    private ZonedDateTime parsearFechaConMultipleFormatos(String fechaStr) {
        try {
            return ZonedDateTime.parse(fechaStr, ORIGINAL_DATE_FORMATTER);
        } catch (Exception ignored) {}
        try {
            return ZonedDateTime.parse(fechaStr, API_DATE_FORMATTER);
        } catch (Exception ignored) {}
        try {
            return ZonedDateTime.parse(fechaStr);
        } catch (Exception ignored) {}
        return Instant.parse(fechaStr).atZone(ZoneId.of("UTC+3"));
    }

    private boolean validarParametros(String driverId, String fecha) {
        return driverId != null && !driverId.isEmpty() && fecha != null && !fecha.isEmpty();
    }

    private boolean verificarCierreRegistrado(String driverId, String dateFrom) {
        if (dateFrom == null || dateFrom.isEmpty() || driverId == null || driverId.isEmpty()) {
            return false;
        }
        try {
            return driverCloseService.obtenerCierrePorDriverIdYFecha(driverId, extraerFechaSolo(dateFrom)).isPresent();
        } catch (Exception e) {
            log.warn("[DriverOrdersService] error verificando cierre: {}", e.getMessage());
            return false;
        }
    }

    private String extraerFechaSolo(String dateFrom) {
        return dateFrom.contains("T")
            ? dateFrom.split("T")[0]
            : dateFrom.substring(0, Math.min(10, dateFrom.length()));
    }

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

    private DriverOrdersResponse crearRespuestaError(String driverId, String dateFrom, String dateTo) {
        FechaRango fechaRango = normalizarFechas(dateFrom, dateTo);
        return DriverOrdersResponse.builder()
            .dateFrom(fechaRango.dateFrom())
            .dateTo(fechaRango.dateTo())
            .orders(new ArrayList<>())
            .cierreRegistrado(verificarCierreRegistrado(driverId, fechaRango.dateFrom()))
            .build();
    }

    private DriverTripsSimplifiedResponse crearRespuestaVaciaViajesSimplificados() {
        return DriverTripsSimplifiedResponse.builder()
            .dateFrom(null).dateTo(null).trips(new ArrayList<>()).build();
    }

    private MultipleDriversTripsSimplifiedResponse.DriverTrips crearDriverTripsVacio(String driverId) {
        return MultipleDriversTripsSimplifiedResponse.DriverTrips.builder()
            .driverId(driverId != null ? driverId : "")
            .trips(new ArrayList<>())
            .build();
    }

    private record CacheEntry(DriverTripsSimplifiedResponse response, long expiresAt) {
        static CacheEntry of(DriverTripsSimplifiedResponse response, long ttlMs) {
            return new CacheEntry(response, System.currentTimeMillis() + ttlMs);
        }
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private record FechaRango(String dateFrom, String dateTo) {}
}
