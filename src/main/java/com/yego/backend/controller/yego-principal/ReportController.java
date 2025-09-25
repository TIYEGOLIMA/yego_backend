package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.service.yego_principal.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para reportes del sistema YEGO Principal
 * Equivalente a ReportsController de NestJS
 */
@Slf4j
@RestController
@RequestMapping("/api/yego-principal/reports")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportService reportService;
    
    /**
     * Obtener estadísticas del sistema
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> getSystemStats(@RequestParam(defaultValue = "30") Integer days) {
        try {
            SystemStatsDto stats = reportService.getSystemStats(days);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas del sistema YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener datos del dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> getDashboardData() {
        try {
            DashboardDataDto dashboardData = reportService.getDashboardData();
            return ResponseEntity.ok(dashboardData);
            
        } catch (Exception e) {
            log.error("Error obteniendo datos del dashboard YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener estadísticas de usuarios
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> getUserStats() {
        try {
            List<UserStatsDto> userStats = reportService.getUserStats();
            return ResponseEntity.ok(userStats);
            
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de usuarios YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Exportar reporte en Excel
     */
    @GetMapping("/export/{type}")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> exportReport(@PathVariable String type,
                                         @RequestParam(defaultValue = "30") Integer days) {
        try {
            byte[] excelData = reportService.exportReport(type, days);
            
            String filename = String.format("reporte-%s-%s.xlsx", type, LocalDate.now());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            
            ByteArrayResource resource = new ByteArrayResource(excelData);
            
            log.info("📊 Reporte YEGO Principal exportado: {} ({} bytes)", type, excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error exportando reporte YEGO Principal {}: {}", type, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
}
