package com.yego.backend.controller.yego_premiun;

import com.yego.backend.entity.yego_premiun.api.response.DriverMonthlyStatsResponse;
import com.yego.backend.service.yego_premiun.DriverMonthlyStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/yego-premiun")
@RequiredArgsConstructor
@Slf4j
public class DriverMonthlyStatsController {

    private final DriverMonthlyStatsService driverMonthlyStatsService;

    @GetMapping("/list")
    public ResponseEntity<List<DriverMonthlyStatsResponse>> obtenerEstadisticas() {
        log.info("🚗 [DriverMonthlyStatsController] Solicitando estadísticas mensuales de conductores");
        List<DriverMonthlyStatsResponse> response = driverMonthlyStatsService.obtenerEstadisticas();
        log.info("✅ [DriverMonthlyStatsController] Registros devueltos: {}", response.size());
        return ResponseEntity.ok(response);
    }
}

