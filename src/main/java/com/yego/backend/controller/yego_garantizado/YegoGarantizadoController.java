package com.yego.backend.controller.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoListResponse;
import com.yego.backend.entity.yego_garantizado.api.response.RegistroCompletoResponse;
import com.yego.backend.service.yego_garantizado.YegoGarantizadoRegistroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/garantizado")
@RequiredArgsConstructor
@Slf4j
public class YegoGarantizadoController {

    private final YegoGarantizadoRegistroService yegoGarantizadoRegistroService;

    /**
     * CONSULTAR semana anterior (ya procesada)
     * GET /api/garantizado/listar-semana-anterior
     */
    @GetMapping("/listar-semana-anterior")
    public ResponseEntity<GarantizadoListResponse> consultarSemanaAnterior() {
        log.info("📋 [YegoGarantizadoController] Recibida solicitud para CONSULTAR semana anterior");
        
        try {
            // Solo consultar datos ya procesados
            GarantizadoListResponse response = yegoGarantizadoRegistroService.listarGarantizadosSemanaAnterior();
            log.info("✅ [YegoGarantizadoController] Consultados {} conductores de la semana anterior", response.getConductores().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoController] Error consultando semana anterior: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * PROCESAR semana anterior (crear/procesar datos)
     * POST /api/garantizado/procesar-semana-anterior
     */
    @PostMapping("/procesar-semana-anterior")
    public ResponseEntity<GarantizadoListResponse> procesarSemanaAnterior() {
        log.info("🔄 [YegoGarantizadoController] Recibida solicitud para PROCESAR semana anterior");
        
        try {
            // Procesar conductores de la semana anterior
            GarantizadoListResponse response = yegoGarantizadoRegistroService.procesarYDevolverSemanaAnterior();
            log.info("✅ [YegoGarantizadoController] Procesados {} conductores de la semana anterior", response.getConductores().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoController] Error procesando semana anterior: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/registros/semana-actual")
    public ResponseEntity<List<RegistroCompletoResponse>> obtenerRegistrosSemanaActual() {
        log.info(" [YegoGarantizadoController] Recibida solicitud para obtener registros de la semana actual");
        
        try {
            List<RegistroCompletoResponse> registros = yegoGarantizadoRegistroService.obtenerRegistrosSemanaActualCompletos();
            log.info(" [YegoGarantizadoController] Encontrados {} registros de la semana actual", registros.size());
            return ResponseEntity.ok(registros);
        } catch (Exception e) {
            log.error(" [YegoGarantizadoController] Error obteniendo registros de la semana actual: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/flota/{flotaId}")
    public ResponseEntity<GarantizadoListResponse> obtenerGarantizadosPorFlota(@PathVariable String flotaId) {
        log.info(" [YegoGarantizadoController] Recibida solicitud para obtener garantizados por flota: {}", flotaId);
        
        try {
            GarantizadoListResponse response = yegoGarantizadoRegistroService.obtenerGarantizadosPorFlota(flotaId);
            log.info(" [YegoGarantizadoController] Encontrados {} garantizados para flota {}", response.getConductores().size(), flotaId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(" [YegoGarantizadoController] Error obteniendo garantizados por flota {}: {}", flotaId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String flotaId,
            @RequestParam(required = false) String estado,
            @RequestParam String semana) {
        
        log.info("⚙️ [YegoGarantizadoController] Recibida solicitud para exportar Excel");
        log.info("📊 Parámetros: flotaId={}, estado={}, semana={}", flotaId, estado, semana);

        try {
            // Delegar toda la lógica al servicio
            byte[] excelBytes = yegoGarantizadoRegistroService.exportarExcel(flotaId, estado, semana);

            // Configurar headers para descarga
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "garantizado_" + semana + ".xlsx");
            headers.setContentLength(excelBytes.length);

            log.info("✅ [YegoGarantizadoController] Excel generado exitosamente");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoController] Error generando Excel: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/marcar-pagado/{id}")
    public ResponseEntity<String> marcarComoPagado(@PathVariable Long id, Authentication authentication) {
        log.info("💰 [YegoGarantizadoController] Recibida solicitud para marcar como pagado el registro ID: {}", id);
        
        try {
            boolean resultado = yegoGarantizadoRegistroService.marcarComoPagado(id, authentication);
            
            if (resultado) {
                return ResponseEntity.ok("Registro marcado como pagado exitosamente");
            } else {
                return ResponseEntity.badRequest().body("No se puede marcar como pagado. Verifique que el conductor esté garantizado.");
            }
            
        } catch (Exception e) {
            log.error("❌ [YegoGarantizadoController] Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error interno del servidor");
        }
    }

}
