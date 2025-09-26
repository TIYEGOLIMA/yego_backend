package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controlador REST para permisos del sistema YEGO Principal
 */
@Slf4j
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {
    
    private final PermissionService permissionService;
    
    /**
     * Crear nuevo permiso
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreatePermissionDto createPermissionDto) {
        PermissionResponseDto permission = permissionService.create(createPermissionDto);
        return ResponseEntity.status(201).body(permission);
    }
    
    /**
     * Obtener todos los permisos
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<List<PermissionResponseDto>> findAll() {
        List<PermissionResponseDto> permissions = permissionService.findAll();
        return ResponseEntity.ok(permissions);
    }
    
    /**
     * Obtener permisos por módulo
     */
    @GetMapping("/module/{module}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<List<PermissionResponseDto>> findByModule(@PathVariable String module) {
        List<PermissionResponseDto> permissions = permissionService.findByModule(module);
        return ResponseEntity.ok(permissions);
    }
    
    /**
     * Obtener permiso por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        PermissionResponseDto permission = permissionService.findOne(id);
        return ResponseEntity.ok(permission);
    }
    
    /**
     * Actualizar permiso
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, 
                                   @Valid @RequestBody UpdatePermissionDto updatePermissionDto) {
        PermissionResponseDto permission = permissionService.update(id, updatePermissionDto);
        return ResponseEntity.ok(permission);
    }
    
    /**
     * Eliminar permiso
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        permissionService.remove(id);
        return ResponseEntity.ok().build();
    }
}