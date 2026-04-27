package com.yego.backend.handler.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MultipleDriversTripsSimplifiedResponse;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_principal.FilteredWebSocketService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FleetDriverNotificationHandler {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final String MODULO_PRO_OPS = "pro-ops";
    private static final String TOPIC_DRIVERS = "/topic/pro-ops/conductores-en-orden";
    private static final String TOPIC_VIAJES = "/topic/pro-ops/viajes-simplificados-en-curso";

    private final FilteredWebSocketService filteredWebSocketService;
    private final WebSocketSessionService webSocketSessionService;
    private final DriverOrdersService driverOrdersService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("[FleetDriverNotificationHandler] executor no terminó en 30s, forzando cierre");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("[FleetDriverNotificationHandler] error cerrando executor", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void enviarConductoresEnOrden(DriversInOrderResponse response) {
        if (response == null) return;
        try {
            Set<String> sesiones = webSocketSessionService.getSessionsWithModuleAccess(MODULO_PRO_OPS);
            if (sesiones.isEmpty()) {
                log.debug("[FleetDriverNotificationHandler] sin sesiones {} conectadas, se omite WS", MODULO_PRO_OPS);
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("type", "DRIVERS_IN_ORDER_UPDATE");
            data.put("conductores", response.getConductores());
            data.put("total", response.getTotal());
            data.put("timestamp", LocalDateTime.now().toString());
            filteredWebSocketService.convertAndSend(TOPIC_DRIVERS, data);

            enviarViajesSimplificadosEnCurso(response);
        } catch (Exception e) {
            log.error("[FleetDriverNotificationHandler] error WS conductores in_order: {}", e.getMessage(), e);
        }
    }

    private void enviarViajesSimplificadosEnCurso(DriversInOrderResponse response) {
        if (response.getConductores() == null || response.getConductores().isEmpty()) return;

        List<String> driverIds = response.getConductores().stream()
            .map(DriversInOrderResponse.DriverInOrderInfo::getId)
            .filter(id -> id != null && !id.isEmpty())
            .collect(Collectors.toList());
        if (driverIds.isEmpty()) return;

        emitirEstado("processing", "Obteniendo viajes simplificados de conductores en curso...",
            new ArrayList<>(), driverIds.size(), null, null);

        CompletableFuture
            .supplyAsync(() -> obtenerViajesSimplificados(driverIds), executorService)
            .thenAccept(viajes -> {
                if (viajes == null || viajes.getDrivers() == null) {
                    emitirEstado("error", "No se pudieron obtener viajes simplificados",
                        new ArrayList<>(), 0, null, null);
                    return;
                }
                emitirEstado("completed", null, viajes.getDrivers(), viajes.getDrivers().size(),
                    viajes.getDateFrom(), viajes.getDateTo());
            });
    }

    private MultipleDriversTripsSimplifiedResponse obtenerViajesSimplificados(List<String> driverIds) {
        try {
            LocalDate hoy = LocalDate.now(LIMA_ZONE);
            String dateFrom = hoy.atStartOfDay(LIMA_ZONE).format(API_DATE_FORMATTER);
            String dateTo = hoy.atTime(23, 59, 59).atZone(LIMA_ZONE).format(API_DATE_FORMATTER);
            return driverOrdersService.obtenerViajesSimplificadosMultiples(driverIds, dateFrom, dateTo);
        } catch (Exception e) {
            log.error("[FleetDriverNotificationHandler] error obteniendo viajes simplificados: {}", e.getMessage(), e);
            return null;
        }
    }

    private void emitirEstado(String status, String message, Object drivers, int totalDrivers,
                              String dateFrom, String dateTo) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "VIAJES_SIMPLIFICADOS_EN_CURSO_UPDATE");
        data.put("status", status);
        if (message != null) data.put("message", message);
        if (dateFrom != null) data.put("date_from", dateFrom);
        if (dateTo != null) data.put("date_to", dateTo);
        data.put("drivers", drivers);
        data.put("total_drivers", totalDrivers);
        data.put("timestamp", LocalDateTime.now().toString());
        filteredWebSocketService.convertAndSend(TOPIC_VIAJES, data);
    }
}
