package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de módulos de atención del sistema YEGO Ticketerera
 */
@RestController
@RequestMapping("/api/ticketera/modulo-atencion")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ModuloAtencionController {
    
    private final ModuloAtencionService moduloAtencionService;
    
    @GetMapping
    public ResponseEntity<List<ModuloAtencion>> obtenerTodosLosModulos() {
        List<ModuloAtencion> modules = moduloAtencionService.obtenerTodosLosModulos();
        return ResponseEntity.ok(modules);
    }
    
    @GetMapping("/activos")
    public ResponseEntity<List<ModuloAtencion>> obtenerModulosActivos() {
        List<ModuloAtencion> modules = moduloAtencionService.obtenerTodosLosModulosActivos();
        return ResponseEntity.ok(modules);
    }

    @GetMapping("/frontend")
    public ResponseEntity<List<ModuloAtencionResponse>> obtenerModulosParaFrontend() {
        log.info("🎯 [ModuloAtencionController] Endpoint /frontend llamado correctamente");
        List<ModuloAtencionResponse> responses = moduloAtencionService.obtenerModulosParaFrontend();
        log.info("✅ [ModuloAtencionController] Devolviendo {} módulos", responses.size());
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{moduleId}/estado/{activo}")
    public ResponseEntity<Void> cambiarEstadoModulo(@PathVariable Long moduleId, @PathVariable boolean activo) {
        moduloAtencionService.cambiarEstadoModulo(moduleId, activo);
        return ResponseEntity.ok().build();
    }
}
