package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.SistemaExternoRequest;
import com.yego.backend.entity.yego_principal.api.request.ToggleActivoRequest;
import com.yego.backend.entity.yego_principal.api.response.SistemaExternoResponse;
import com.yego.backend.entity.yego_principal.api.response.ToggleActivoResponse;
import com.yego.backend.entity.yego_principal.entities.SistemaExterno;
import com.yego.backend.service.yego_principal.SistemaExternoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controlador REST para sistemas externos del sistema YEGO Principal
 */
@Slf4j
@RestController
@RequestMapping("/api/sistemas-externos")
@RequiredArgsConstructor
public class SistemaExternoController {
    
    private final SistemaExternoService sistemaExternoService;
    
    /**
     * Obtener todos los sistemas externos
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR')")
    public ResponseEntity<List<SistemaExternoResponse>> obtenerTodos() {
        List<SistemaExternoResponse> sistemas = sistemaExternoService.obtenerTodos();
        return ResponseEntity.status(200).body(sistemas);
    }
    
    /**
     * Obtener sistema externo por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR')")
    public ResponseEntity<SistemaExternoResponse> obtenerPorId(@PathVariable Long id) {
        SistemaExternoResponse sistema = sistemaExternoService.obtenerPorId(id);
        return ResponseEntity.status(200).body(sistema);
    }
    
    /**
     * Crear nuevo sistema externo
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<SistemaExternoResponse> crear(@Valid @RequestBody SistemaExternoRequest request) {
        SistemaExternoResponse sistema = sistemaExternoService.crear(request);
        return ResponseEntity.status(201).body(sistema);
    }
    
    /**
     * Actualizar sistema externo
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<SistemaExternoResponse> actualizar(@PathVariable Long id, 
                                                           @Valid @RequestBody SistemaExternoRequest request) {
        SistemaExternoResponse sistema = sistemaExternoService.actualizar(id, request);
        return ResponseEntity.status(200).body(sistema);
    }
    
    /**
     * Cambiar estado de un sistema externo
     */
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<SistemaExternoResponse> cambiarEstado(@PathVariable Long id, 
                                                              @RequestParam SistemaExterno.EstadoSistema estado) {
        SistemaExternoResponse sistema = sistemaExternoService.cambiarEstado(id, estado);
        return ResponseEntity.status(200).body(sistema);
    }
    
    /**
     * Verificar estado de un sistema externo
     */
    @PostMapping("/{id}/verificar")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR')")
    public ResponseEntity<SistemaExternoResponse> verificarEstado(@PathVariable Long id) {
        SistemaExternoResponse sistema = sistemaExternoService.verificarEstado(id);
        return ResponseEntity.status(200).body(sistema);
    }
    
    /**
     * Eliminar sistema externo
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        sistemaExternoService.eliminar(id);
        return ResponseEntity.status(204).build();
    }
    
    /**
     * Buscar sistemas externos por término
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN') or hasRole('OPERADOR')")
    public ResponseEntity<List<SistemaExternoResponse>> buscarPorTermino(@RequestParam String termino) {
        List<SistemaExternoResponse> sistemas = sistemaExternoService.buscarPorTermino(termino);
        return ResponseEntity.status(200).body(sistemas);
    }

    /**
     * Cambiar estado activo de un sistema externo
     */
    @PutMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ToggleActivoResponse> toggleActivo(@PathVariable Long id, 
                                                           @Valid @RequestBody ToggleActivoRequest request) {
        SistemaExternoResponse sistema = sistemaExternoService.toggleActivo(id, request.getActivo());
        
        ToggleActivoResponse response = ToggleActivoResponse.builder()
                .success(true)
                .message(String.format("Sistema %s correctamente", 
                        request.getActivo() ? "activado" : "desactivado"))
                .data(ToggleActivoResponse.SistemaExternoData.builder()
                        .id(sistema.getId())
                        .nombre(sistema.getNombre())
                        .activo(sistema.getActivo())
                        .url(sistema.getUrl())
                        .build())
                .build();
        
        return ResponseEntity.status(200).body(response);
    }
}
