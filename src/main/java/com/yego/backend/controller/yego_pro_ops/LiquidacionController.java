package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionSemanalResponse;
import com.yego.backend.service.yego_pro_ops.LiquidacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

    @PostMapping("/{driverId}/liquidar")
    public Map<String, Object> liquidarSemana(@PathVariable String driverId) {
        log.info("[LiquidacionController] liquidar semana driverId={}", driverId);
        return liquidacionService.liquidarSemana(driverId);
    }
}
