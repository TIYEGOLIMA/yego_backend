package com.yego.backend.controller.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.request.CalcularGarantizadoRequest;
import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoListResponse;
import com.yego.backend.service.yego_garantizado.CalculoGarantizadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para cálculos de garantizado
 */
@Slf4j
@RestController
@RequestMapping("/api/garantizado/configuraciones")
@RequiredArgsConstructor
public class CalculoGarantizadoController {

    private final CalculoGarantizadoService calculoService;

    /**
     * 🚀 MÉTODO PRINCIPAL: Guardar configuraciones Y PROCESAR conductores
     * POST /api/garantizado/configuraciones/procesar
     * Si no se envía semana, se usa la semana anterior automáticamente
     * @return Lista de conductores procesados (para enviar por WebSocket)
     */
    @PostMapping("/procesar")
    public ResponseEntity<GarantizadoListResponse> guardarConfiguracionesYProcesar(
            @RequestBody CalcularGarantizadoRequest request) {
        log.info("🚀 [CalculoGarantizadoController] Solicitud para GUARDAR Y PROCESAR");
        
        try {
            GarantizadoListResponse response = calculoService.guardarConfiguracionesYProcesar(request);
            log.info("✅ [CalculoGarantizadoController] Configuraciones guardadas Y {} conductores procesados", 
                response.getConductores().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [CalculoGarantizadoController] Error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Guardar solo configuraciones (sin procesar)
     * POST /api/garantizado/configuraciones/guardar
     * Si no se envía semana, se usa la semana anterior automáticamente
     */
    @PostMapping("/guardar")
    public ResponseEntity<?> guardarConfiguraciones(
            @RequestBody CalcularGarantizadoRequest request) {
        log.info("📥 [CalculoGarantizadoController] Solicitud para guardar configuraciones");
        
        try {
            calculoService.guardarConfiguraciones(request);
            log.info("✅ [CalculoGarantizadoController] Configuraciones guardadas exitosamente");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ [CalculoGarantizadoController] Error guardando configuraciones: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

}

