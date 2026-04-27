package com.yego.backend.scheduler.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import com.yego.backend.handler.yego_pro_ops.FleetDriverNotificationHandler;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class CalculatedShiftScheduler {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final String MODULO_PRO_OPS = "pro-ops";

    private final FleetDriverService fleetDriverService;
    private final CalculatedShiftService calculatedShiftService;
    private final FleetDriverNotificationHandler fleetDriverNotificationHandler;
    private final WebSocketSessionService webSocketSessionService;

    @Scheduled(cron = "0 10 8 * * *", zone = "America/Lima")
    public void calcularHorasTurnoDiaAnterior() {
        LocalDate fechaAnterior = LocalDate.now(LIMA_ZONE).minusDays(1);
        log.info("[CalculatedShiftScheduler] inicio batch subturnos día anterior fecha={}", fechaAnterior);
        try {
            calculatedShiftService.procesarHorasTurnoDiaAnterior();
            log.info("[CalculatedShiftScheduler] batch completado fecha={}", fechaAnterior);
        } catch (Exception e) {
            log.error("[CalculatedShiftScheduler] error batch fecha={}: {}", fechaAnterior, e.getMessage(), e);
        }
    }

    @Scheduled(
        initialDelayString = "${yego.pro-ops.conductores-en-orden-ws.initial-delay-ms:60000}",
        fixedDelayString = "${yego.pro-ops.conductores-en-orden-ws.fixed-delay-ms:300000}",
        zone = "America/Lima")
    public void actualizarConductoresEnOrden() {
        Set<String> sesiones = webSocketSessionService.getSessionsWithModuleAccess(MODULO_PRO_OPS);
        if (sesiones.isEmpty()) {
            log.debug("[CalculatedShiftScheduler] sin sesiones {} conectadas, se omite WS", MODULO_PRO_OPS);
            return;
        }
        try {
            DriversInOrderResponse response = fleetDriverService.obtenerConductoresEnOrden();
            if (response == null) {
                log.warn("[CalculatedShiftScheduler] respuesta null al obtener conductores in_order");
                return;
            }
            fleetDriverNotificationHandler.enviarConductoresEnOrden(response);
            log.debug("[CalculatedShiftScheduler] WS conductores in_order enviados sesiones={} total={}",
                sesiones.size(), response.getTotal());
        } catch (Exception e) {
            log.error("[CalculatedShiftScheduler] error WS conductores in_order: {}", e.getMessage(), e);
        }
    }
}
