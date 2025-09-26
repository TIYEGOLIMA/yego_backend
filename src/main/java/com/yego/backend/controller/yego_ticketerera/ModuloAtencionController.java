package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de módulos de atención del sistema YEGO Ticketerera
 */
@RestController
@RequestMapping("/api/ticketerera/modulo-atencion")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ModuloAtencionController {
    
    private final ModuloAtencionService moduloAtencionService;
    
    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR')")
    public ResponseEntity<List<ModuloAtencion>> obtenerTodosLosModulos() {
        List<ModuloAtencion> modules = moduloAtencionService.obtenerTodosLosModulos();
        return ResponseEntity.ok(modules);
    }
    
    @GetMapping("/activos")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR')")
    public ResponseEntity<List<ModuloAtencion>> obtenerModulosActivos() {
        List<ModuloAtencion> modules = moduloAtencionService.obtenerTodosLosModulosActivos();
        return ResponseEntity.ok(modules);
    }

    @GetMapping("/frontend")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR')")
    public ResponseEntity<List<ModuloAtencionResponse>> obtenerModulosParaFrontend() {
        List<ModuloAtencionResponse> responses = moduloAtencionService.obtenerModulosParaFrontend();
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{moduleId}/estado/{activo}")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> cambiarEstadoModulo(@PathVariable Long moduleId, @PathVariable boolean activo) {
        moduloAtencionService.cambiarEstadoModulo(moduleId, activo);
        return ResponseEntity.ok().build();
    }
}
