package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
import com.yego.backend.service.yego_ticketerera.SacStatsExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SacStatsResponse> obtenerTodasLasEstadisticas(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        log.info("[SacStats] Estadísticas SAC - fechaInicio: {}, fechaFin: {}", fechaInicio, fechaFin);
        SacStatsResponse stats = sacStatsService.obtenerTodasLasEstadisticas(fechaInicio, fechaFin);
        return ResponseEntity.ok(stats);
    }
        
    @GetMapping("/all")
    public ResponseEntity<SacStatsResponse> obtenerTodasLasEstadisticasSinFiltro() {
        log.info("[SacStats] Estadísticas SAC sin filtro de fecha");
        SacStatsResponse stats = sacStatsService.obtenerTodasLasEstadisticas(null, null);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportarAExcel(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        log.info("[SacStats] Exportar Excel - fechaInicio: {}, fechaFin: {}", fechaInicio, fechaFin);
        return sacStatsExportService.exportarAExcel(fechaInicio, fechaFin);
    }
    
    @GetMapping("/export/image/{formato}")
    public ResponseEntity<byte[]> exportarAImagen(
            @PathVariable String formato,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        log.info("[SacStats] Exportar imagen {} - fechaInicio: {}, fechaFin: {}", formato, fechaInicio, fechaFin);
        return sacStatsExportService.exportarAImagen(formato, fechaInicio, fechaFin);
    }
}
