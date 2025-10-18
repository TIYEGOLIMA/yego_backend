package com.yego.backend.controller.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.service.yego_garantizado.FlotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flota")
@RequiredArgsConstructor
@Slf4j
public class FlotaController {

    private final FlotaService flotaService;

    @GetMapping("/todas")
    public ResponseEntity<List<FlotaResponse>> obtenerTodasLasFlotas() {
        log.info("⚙️ [FlotaController] Recibida solicitud para obtener todas las flotas");
        
        try {
            List<FlotaResponse> flotas = flotaService.obtenerFlotas();
            log.info("✅ [FlotaController] Encontradas {} flotas", flotas.size());
            return ResponseEntity.ok(flotas);
        } catch (Exception e) {
            log.error("❌ [FlotaController] Error obteniendo flotas: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
