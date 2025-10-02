package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
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

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR')")
    public ResponseEntity<SacStatsResponse> obtenerTodasLasEstadisticas() {
        log.info("📊 [SacStats] Obteniendo TODAS las estadísticas de SAC");
        SacStatsResponse stats = sacStatsService.obtenerTodasLasEstadisticas();
        return ResponseEntity.ok(stats);
    }
}
