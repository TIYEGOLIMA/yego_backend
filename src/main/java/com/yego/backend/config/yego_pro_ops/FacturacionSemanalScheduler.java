package com.yego.backend.config.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.ResumenSemanalResponse;
import com.yego.backend.entity.yego_pro_ops.entities.FacturacionSemanal;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FacturacionSemanalScheduler {

    private final CalculatedShiftService calculatedShiftService;
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Todos los lunes a las 8:00 AM (hora Lima) genera la facturación semanal
     * de la semana anterior y la guarda como pendiente de pago.
     */
    @Scheduled(cron = "0 0 10 * * MON", zone = "America/Lima")
    public void generarFacturacionSemanalAutomatica() {
        log.info("[FacturacionScheduler] Iniciando generación automática de facturación semanal");
        try {
            LocalDate hoy = LocalDate.now();
            LocalDate lunesAnterior = hoy.minusWeeks(1).with(DayOfWeek.MONDAY);
            LocalDate domingoAnterior = lunesAnterior.plusDays(6);

            String inicio = lunesAnterior.format(YMD);
            String fin = domingoAnterior.format(YMD);

            log.info("[FacturacionScheduler] Semana anterior: {} al {}", inicio, fin);

            ResumenSemanalResponse resumen = calculatedShiftService.obtenerResumenSemanal(inicio, fin);

            if (resumen.getConductores() == null || resumen.getConductores().isEmpty()) {
                log.info("[FacturacionScheduler] Sin conductores para la semana {}/{}", inicio, fin);
                return;
            }

            List<FacturacionSemanal> entities = resumen.getConductores().stream()
                .map(c -> FacturacionSemanal.builder()
                    .driverId(c.getDriverId())
                    .fechaInicio(lunesAnterior)
                    .fechaFin(domingoAnterior)
                    .totalViajes(c.getTotalViajes())
                    .viajesValidos(c.getViajesValidos())
                    .horasTrabajo(c.getHorasTrabajo())
                    .montoTotalProducido(BigDecimal.valueOf(c.getMontoTotalProducido()))
                    .comisionApp(BigDecimal.valueOf(c.getComisionApp()))
                    .montoNeto(BigDecimal.valueOf(c.getMontoNeto()))
                    .kmRecorrido(BigDecimal.valueOf(c.getKmRecorrido()))
                    .gastoCombustible(BigDecimal.valueOf(c.getGastoCombustible()))
                    .gastoMantenimiento(BigDecimal.valueOf(c.getGastoMantenimiento()))
                    .produccionBonificable(BigDecimal.valueOf(c.getProduccionBonificable()))
                    .bonoAdicViajes(BigDecimal.valueOf(c.getBonoAdicViajes()))
                    .bono(BigDecimal.valueOf(c.getBono()))
                    .porcentajePago(c.getPorcentajePago())
                    .pago(BigDecimal.valueOf(c.getPago()))
                    .pagoTotal(BigDecimal.valueOf(c.getPagoTotal()))
                    .utilidad(BigDecimal.valueOf(c.getUtilidad()))
                    .utilidadPorViaje(BigDecimal.valueOf(c.getUtilidadPorViaje()))
                    .pagoPorViaje(BigDecimal.valueOf(c.getPagoPorViaje()))
                    .diasTrabajados(c.getDiasTrabajados())
                    .diasLiquidados(c.getDiasLiquidados())
                    .turno(c.getTurno())
                    .estado("pendiente")
                    .build())
                .collect(Collectors.toList());

            int generados = 0;
            int omitidos = 0;
            for (FacturacionSemanal entity : entities) {
                if (!entity.getDiasLiquidados().equals(entity.getDiasTrabajados())
                        || entity.getDiasTrabajados() == 0) {
                    log.warn("[FacturacionScheduler] Omitido conductor {}: días liquidados {}/{} — faltan cierres diarios",
                        entity.getDriverId(), entity.getDiasLiquidados(), entity.getDiasTrabajados());
                    omitidos++;
                    continue;
                }
                calculatedShiftService.registrarFacturacionSemanal(entity);
                generados++;
            }

            log.info("[FacturacionScheduler] {} generados, {} omitidos (faltan cierres) para semana {}/{}",
                generados, omitidos, inicio, fin);
        } catch (Exception e) {
            log.error("[FacturacionScheduler] Error generando facturación semanal: {}", e.getMessage(), e);
        }
    }
}
