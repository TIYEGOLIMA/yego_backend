package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.service.yego_ticketerera.SacStatsExportService;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ticketera/sac-stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SacStatsController {

    private final SacStatsService sacStatsService;
    private final SacStatsExportService sacStatsExportService;

    @GetMapping
    public ResponseEntity<SacStatsResponse> obtenerEstadisticas(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(sacStatsService.obtenerTodasLasEstadisticas(fechaInicio, fechaFin, sedeId));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportarAExcel(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long sedeId) {
        return sacStatsExportService.exportarAExcel(fechaInicio, fechaFin, sedeId);
    }

    @GetMapping("/export/image/{formato}")
    public ResponseEntity<byte[]> exportarAImagen(
            @PathVariable String formato,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) Long sedeId) {
        return sacStatsExportService.exportarAImagen(formato, fechaInicio, fechaFin, sedeId);
    }
}
