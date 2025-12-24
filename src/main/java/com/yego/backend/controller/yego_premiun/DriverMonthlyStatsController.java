package com.yego.backend.controller.yego_premiun;

import com.yego.backend.entity.yego_premiun.api.request.DriverMonthlyStatsProcessRequest;
import com.yego.backend.entity.yego_premiun.api.response.DriverMonthlyStatsResponse;
import com.yego.backend.service.yego_premiun.DriverMonthlyStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/yego-premiun")
@RequiredArgsConstructor
@Slf4j
public class DriverMonthlyStatsController {

    private final DriverMonthlyStatsService driverMonthlyStatsService;

    @PostMapping("/driver-active/process")
    public ResponseEntity<List<DriverMonthlyStatsResponse>> procesarActivos(@Valid @RequestBody DriverMonthlyStatsProcessRequest request) {
        log.info("🚗 [DriverMonthlyStatsController] Solicitando procesamiento de driver_active_list para mes={}, year={}", request.getMonth(), request.getYear());
        List<DriverMonthlyStatsResponse> response = driverMonthlyStatsService.procesarYListarActivos(request.getMonth(), request.getYear());
        log.info("✅ [DriverMonthlyStatsController] Procesamiento completado, registros devueltos: {}", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/driver-active/list")
    public ResponseEntity<List<DriverMonthlyStatsResponse>> listarActivos() {
        log.info("📄 [DriverMonthlyStatsController] Solicitando listado de driver_active_list");
        List<DriverMonthlyStatsResponse> response = driverMonthlyStatsService.listarActivos();
        log.info("✅ [DriverMonthlyStatsController] Registros devueltos: {}", response.size());
        return ResponseEntity.ok(response);
    }
}

