package com.yego.backend.scheduler.yego_pro_ops;

import com.yego.backend.handler.yego_pro_ops.FleetDriverNotificationHandler;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class CalculatedShiftScheduler {

    private final FleetDriverService fleetDriverService;
    private final CalculatedShiftService calculatedShiftService;
    private final FleetDriverNotificationHandler fleetDriverNotificationHandler;
    private final WebSocketSessionService webSocketSessionService;
    
    private static final ZoneId ZONE_UTC_MINUS_5 = ZoneId.of("America/Lima");

/**
     * Scheduler para calcular y guardar las horas de turno del día anterior
     * Se ejecuta todos los días a las 8:10 AM (America/Lima) para procesar el día anterior
     * Cron: 0 10 8 * * * (segundo=0, minuto=10, hora=8)
     *
     * Este scheduler procesa TODOS los conductores uno por uno:
     * - Calcula turnos diurnos y nocturnos del día anterior
     * - Guarda los turnos calculados automáticamente
     * - Omite conductores que ya tienen turnos manuales registrados
     * - Incluye delay entre conductores para evitar saturar la API
     */
    @Scheduled(cron = "0 10 8 * * *", zone = "America/Lima")
    public void calcularHorasTurnoDiaAnterior() {
        LocalDateTime ahora = LocalDateTime.now(ZONE_UTC_MINUS_5);
        LocalDate fechaAnterior = ahora.toLocalDate().minusDays(1);
        
        log.info("⏰ [CalculatedShiftScheduler] ⏰⏰⏰ SCHEDULER DIARIO EJECUTÁNDOSE (8:10 AM) - {} ⏰⏰⏰", ahora);
        log.info("📅 [CalculatedShiftScheduler] Procesando turnos del día anterior: {}", fechaAnterior);
        
        try {
            log.info("🕐 [CalculatedShiftScheduler] Iniciando cálculo de horas de turno del día anterior para TODOS los conductores");
            log.info("📊 [CalculatedShiftScheduler] El proceso calculará turnos diurnos y nocturnos para cada conductor");
            
            calculatedShiftService.procesarHorasTurnoDiaAnterior();
            
            log.info("✅ [CalculatedShiftScheduler] Cálculo de horas de turno del día anterior completado exitosamente");
            log.info("📈 [CalculatedShiftScheduler] Todos los turnos del día {} han sido procesados y guardados", fechaAnterior);
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftScheduler] Error ejecutando cálculo de horas de turno del día anterior: {}", e.getMessage(), e);
            log.error("❌ [CalculatedShiftScheduler] Fecha que se intentó procesar: {}", fechaAnterior);
            e.printStackTrace();
        }
    }

    /**
     * Scheduler para actualizar conductores con status "in_order" y enviarlos por WebSocket.
     * Intervalo configurable en application.properties (yego.pro-ops.conductores-en-orden-ws.*).
     * Solo corre si hay al menos un usuario con acceso a pro-ops conectado por WebSocket.
     */
    @Scheduled(initialDelayString = "${yego.pro-ops.conductores-en-orden-ws.initial-delay-ms:60000}", fixedDelayString = "${yego.pro-ops.conductores-en-orden-ws.fixed-delay-ms:300000}", zone = "America/Lima")
    public void actualizarConductoresEnOrden() {
        LocalDateTime ahora = LocalDateTime.now(ZONE_UTC_MINUS_5);
        log.info("⏰ [CalculatedShiftScheduler] ⏰⏰⏰ SCHEDULER WEBSOCKET EJECUTÁNDOSE - {} ⏰⏰⏰", ahora);
        
        Set<String> sessionsWithAccess = webSocketSessionService.getSessionsWithModuleAccess("pro-ops");
        if (sessionsWithAccess.isEmpty()) {
            log.info("⏭️ [CalculatedShiftScheduler] No hay usuarios con acceso a pro-ops - omitiendo actualización de conductores en orden");
            return;
        }
        
        log.info("🚗 [CalculatedShiftScheduler] Actualizando conductores con status 'in_order' - {} usuarios conectados", sessionsWithAccess.size());
        try {
            var response = fleetDriverService.obtenerConductoresEnOrden();
            
            // Enviar datos por WebSocket a todos los clientes conectados
            if (response != null) {
                log.info("📤 [CalculatedShiftScheduler] Enviando {} conductores en orden por WebSocket", response.getTotal());
                fleetDriverNotificationHandler.enviarConductoresEnOrden(response);
            }
            
            log.info("✅ [CalculatedShiftScheduler] Actualización de conductores en orden completada - Total: {} conductores", response != null ? response.getTotal() : 0);
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftScheduler] Error actualizando conductores en orden: {}", e.getMessage(), e);
        }
    }

}

