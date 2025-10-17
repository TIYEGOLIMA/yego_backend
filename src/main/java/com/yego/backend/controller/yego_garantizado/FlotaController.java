package com.yego.backend.controller.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.service.yego_garantizado.FlotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para flotas del sistema YEGO Garantizado
 */
@Slf4j
@RestController
@RequestMapping("/api/garantizado/flotas")
@RequiredArgsConstructor
public class FlotaController {
    
    private final FlotaService flotaService;
    
    /**
     * Obtener todas las flotas de Yego
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<List<FlotaResponse>> obtenerFlotas() {
        List<FlotaResponse> flotas = flotaService.obtenerFlotas();
        return ResponseEntity.ok(flotas);
    }
}
