package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
import com.yego.backend.service.yego_ticketerera.SacStatsExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para estadísticas de SAC (Servicio al Cliente)
 */
@RestController
@RequestMapping("/api/ticketera/sac-stats")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SacStatsController {
    
    private final SacStatsService sacStatsService;
    private final SacStatsExportService sacStatsExportService;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<SacStatsResponse> obtenerTodasLasEstadisticas() {
        log.info("📊 [SacStats] Obteniendo TODAS las estadísticas de SAC");
        SacStatsResponse stats = sacStatsService.obtenerTodasLasEstadisticas();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/export/excel")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<byte[]> exportarAExcel() {
        log.info("📊 [SacStats] Exportando estadísticas a Excel");
        return sacStatsExportService.exportarAExcel();
    }
    
    @GetMapping("/export/image/{formato}")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<byte[]> exportarAImagen(@PathVariable String formato) {
        log.info("📊 [SacStats] Exportando estadísticas a imagen: {}", formato);
        return sacStatsExportService.exportarAImagen(formato);
    }
}
