package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.LiquidarRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionPendienteResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionSemanalResponse;
import com.yego.backend.service.yego_pro_ops.LiquidacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pro-ops/liquidacion")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LiquidacionController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATETIME_SHORT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final LiquidacionService liquidacionService;

    @GetMapping("/{driverId}/semanal")
    public LiquidacionSemanalResponse getLiquidacionSemanal(
            @PathVariable String driverId,
            @RequestParam(required = false) String weekStart) {
        LocalDate start;
        if (weekStart != null && !weekStart.isEmpty()) {
            start = LocalDate.parse(weekStart, DATE_FORMATTER);
        } else {
            start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        log.info("[LiquidacionController] consulta semanal driverId={} weekStart={}", driverId, start);
        return liquidacionService.getLiquidacionSemanal(driverId, start);
    }

    @GetMapping("/{driverId}/pendiente")
    public LiquidacionPendienteResponse getLiquidacionPendiente(
            @PathVariable String driverId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        LocalDateTime d = parseDateTime(desde);
        LocalDateTime h = parseDateTime(hasta);
        log.info("[LiquidacionController] consulta pendiente driverId={} desde={} hasta={}", driverId, d, h);
        return liquidacionService.getLiquidacionPendiente(driverId, d, h);
    }

    @PostMapping("/{driverId}/liquidar")
    public Map<String, Object> liquidarPendiente(
            @PathVariable String driverId,
            @Valid @RequestBody LiquidarRequest request) {
        request.setDriverId(driverId);
        log.info("[LiquidacionController] liquidar pendiente driverId={} userId={} desde={} hasta={}", driverId, request.getUserId(), request.getDesde(), request.getHasta());
        return liquidacionService.liquidarPendiente(request);
    }

    @PostMapping("/admin/backfill-producido")
    public Map<String, Object> backfillProducido() {
        log.info("[LiquidacionController] backfill producido histórico");
        return liquidacionService.backfillProducido();
    }

    @DeleteMapping("/{driverId}/limpiar")
    public void limpiarFacturacion(
            @PathVariable String driverId,
            @RequestParam String desde,
            @RequestParam String hasta) {
        LocalDate d = LocalDate.parse(desde, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate h = LocalDate.parse(hasta, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("[LiquidacionController] limpiar facturación driverId={} desde={} hasta={}", driverId, d, h);
        liquidacionService.limpiarFacturacion(driverId, d, h);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDateTime.parse(value, DATETIME_FORMATTER);
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(value, DATETIME_SHORT_FORMATTER);
            } catch (Exception e2) {
                log.warn("[LiquidacionController] no se pudo parsear fecha: {}", value);
                return null;
            }
        }
    }
}
