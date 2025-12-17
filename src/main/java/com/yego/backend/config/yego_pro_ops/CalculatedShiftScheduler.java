package com.yego.backend.config.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverInfoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverKpiResponse;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class CalculatedShiftScheduler {

    private final FleetDriverService fleetDriverService;
    private final CalculatedShiftService calculatedShiftService;
    
    private static final ZoneId ZONE_UTC_MINUS_5 = ZoneId.of("America/Lima");
    private final Map<String, LocalDateTime> primeraVezVistoActivoHoy = new ConcurrentHashMap<>();
    private final Map<String, LocalDate> ultimoDiaVisto = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 5000)
    public void monitorearYGuardarTurnos() {
        try {
            DriverKpiResponse kpis = fleetDriverService.consultarConductores();
            LocalDateTime ahora = LocalDateTime.now(ZONE_UTC_MINUS_5);
            LocalDate fechaActual = ahora.toLocalDate();
            Set<String> conductoresActivosAhora = new HashSet<>();
            
            if (kpis != null && kpis.getItems() != null && !kpis.getItems().isEmpty()) {
                for (DriverInfoResponse driverInfo : kpis.getItems()) {
                    if (driverInfo.getDriverId() == null) {
                        continue;
                    }
                    
                    String driverId = driverInfo.getDriverId();
                    conductoresActivosAhora.add(driverId);
                    
                    LocalDate ultimoDia = ultimoDiaVisto.get(driverId);
                    if (ultimoDia == null || !ultimoDia.equals(fechaActual)) {
                        primeraVezVistoActivoHoy.put(driverId, ahora);
                        ultimoDiaVisto.put(driverId, fechaActual);
                    }
                    
                    try {
                        calculatedShiftService.actualizarTurnoConductor(
                            driverId,
                            driverInfo.getStatus(),
                            driverInfo.getStatusDuration(),
                            primeraVezVistoActivoHoy.get(driverId)
                        );
                    } catch (Exception e) {
                        log.error("❌ Error procesando turno para driver_id {}: {}", driverId, e.getMessage());
                    }
                }
            }
            
            calculatedShiftService.verificarYFinalizarTurnosDesconectados(conductoresActivosAhora);
            limpiarMapaAntiguo(fechaActual);
            
        } catch (Exception e) {
            log.error("❌ Error en monitoreo de turnos: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduler para calcular y guardar las horas de turno del día anterior
     * Se ejecuta todos los días a las 7:48 AM para procesar el día anterior
     * Cron: 0 48 7 * * * (segundo minuto hora día mes día_semana)
     */
    @Scheduled(cron = "0 19 8 * * *", zone = "America/Lima")
    public void calcularHorasTurnoDiaAnterior() {
        LocalDateTime ahora = LocalDateTime.now(ZONE_UTC_MINUS_5);
        log.info("⏰ [CalculatedShiftScheduler] ⏰⏰⏰ SCHEDULER EJECUTÁNDOSE A LAS {} ⏰⏰⏰", ahora);
        try {
            log.info("🕐 [CalculatedShiftScheduler] Iniciando cálculo de horas de turno del día anterior");
            calculatedShiftService.procesarHorasTurnoDiaAnterior();
            log.info("✅ [CalculatedShiftScheduler] Cálculo de horas de turno del día anterior completado");
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftScheduler] Error ejecutando cálculo de horas de turno del día anterior: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void limpiarMapaAntiguo(LocalDate fechaActual) {
        primeraVezVistoActivoHoy.entrySet().removeIf(entry -> {
            LocalDate ultimoDia = ultimoDiaVisto.get(entry.getKey());
            return ultimoDia == null || !ultimoDia.equals(fechaActual);
        });
        ultimoDiaVisto.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBefore(fechaActual));
    }
}

