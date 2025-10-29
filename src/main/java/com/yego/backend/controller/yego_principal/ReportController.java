package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para reportes del sistema YEGO Principal
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportService reportService;
    
    /**
     * Obtener estadísticas del sistema
     */
    @GetMapping("/system/stats")
    public ResponseEntity<?> getSystemStats(@RequestParam(defaultValue = "30") Integer days) {
        SystemStatsDto stats = reportService.getSystemStats(days);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener datos del dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData() {
        DashboardDataDto data = reportService.getDashboardData();
        return ResponseEntity.ok(data);
    }
    
    /**
     * Obtener estadísticas de usuarios
     */
    @GetMapping("/users/stats")
    public ResponseEntity<List<UserStatsDto>> getUserStats() {
        List<UserStatsDto> stats = reportService.getUserStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener estadísticas semanales
     */
    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyStats() {
        WeeklyStatsDto stats = reportService.getWeeklyStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Exportar reporte
     */
    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> exportReport(@PathVariable String type,
                                              @RequestParam(defaultValue = "30") Integer days) {
        byte[] report = reportService.exportReport(type, days);
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=" + type + "_report.xlsx")
                .body(report);
    }
}