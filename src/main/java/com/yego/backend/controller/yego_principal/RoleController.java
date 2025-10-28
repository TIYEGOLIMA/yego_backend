package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controlador REST para roles del sistema YEGO Principal
 */
@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    
    private final RoleService roleService;
    
    /**
     * Crear nuevo rol
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateRoleDto createRoleDto) {
        RoleResponseDto role = roleService.create(createRoleDto);
        return ResponseEntity.status(201).body(role);
    }
    
    /**
     * Obtener todos los roles (completo - solo para admin)
     */
    @GetMapping("/find-all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<List<RoleResponseDto>> findAll() {
        List<RoleResponseDto> roles = roleService.findAll();
        return ResponseEntity.ok(roles);
    }
    
    /**
     * Obtener roles activos (solo id y name - para formularios)
     */
    @GetMapping("/find-all-active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<List<RoleSimpleDto>> findAllActive() {
        List<RoleSimpleDto> roles = roleService.findAllActive();
        return ResponseEntity.ok(roles);
    }
    
    
    /**
     * Obtener rol por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        RoleResponseDto role = roleService.findOne(id);
        return ResponseEntity.ok(role);
    }
    
    /**
     * Actualizar rol
     */
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, 
                                   @Valid @RequestBody UpdateRoleDto updateRoleDto) {
        RoleResponseDto role = roleService.update(id, updateRoleDto);
        return ResponseEntity.ok(role);
    }
    
    /**
     * Eliminar rol
     */
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        roleService.remove(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Activar/Desactivar rol
     */
    @PutMapping("/toggle-status/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        RoleResponseDto role = roleService.toggleStatus(id);
        return ResponseEntity.ok(role);
    }
}