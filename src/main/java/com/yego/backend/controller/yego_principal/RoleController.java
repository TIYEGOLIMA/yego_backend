package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.RoleService;
import com.yego.backend.service.yego_principal.ModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    private final ModuleService moduleService;
    
    /**
     * Crear nuevo rol
     */
    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody CreateRoleDto createRoleDto) {
        RoleResponseDto role = roleService.create(createRoleDto);
        return ResponseEntity.status(201).body(role);
    }
    
    /**
     * Obtener todos los roles (completo - solo para admin)
     */
    @GetMapping("/find-all")
    public ResponseEntity<List<RoleResponseDto>> findAll() {
        List<RoleResponseDto> roles = roleService.findAll();
        return ResponseEntity.ok(roles);
    }
    
    /**
     * Obtener roles activos (solo id y name - para formularios)
     */
    @GetMapping("/find-all-active")
    public ResponseEntity<List<RoleSimpleDto>> findAllActive() {
        List<RoleSimpleDto> roles = roleService.findAllActive();
        return ResponseEntity.ok(roles);
    }
    
    
    /**
     * Obtener rol por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        RoleResponseDto role = roleService.findOne(id);
        return ResponseEntity.ok(role);
    }
    
    /**
     * Actualizar rol
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, 
                                   @Valid @RequestBody UpdateRoleDto updateRoleDto) {
        RoleResponseDto role = roleService.update(id, updateRoleDto);
        return ResponseEntity.ok(role);
    }
    
    /**
     * Eliminar rol
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        roleService.remove(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Activar/Desactivar rol
     */
    @PutMapping("/toggle-status/{id}")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        RoleResponseDto role = roleService.toggleStatus(id);
        return ResponseEntity.ok(role);
    }
    
    /**
     * Obtener módulos disponibles para asignar a roles
     * Útil para formularios de creación/edición de roles
     */
    @GetMapping("/available-modules")
    public ResponseEntity<List<ModuleResponse>> getAvailableModules() {
        log.info("📋 [RoleController] Recibida solicitud para obtener módulos disponibles para roles");
        try {
            List<ModuleResponse> modules = moduleService.obtenerActivos();
            log.info("✅ [RoleController] Devueltos {} módulos disponibles para asignar a roles", modules.size());
            return ResponseEntity.ok(modules);
        } catch (Exception e) {
            log.error("❌ [RoleController] Error obteniendo módulos disponibles: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtener módulos con sus acciones disponibles para crear/editar roles
     * Devuelve módulos activos agrupados con las acciones disponibles desde la tabla permissions
     */
    @GetMapping("/modules-with-actions")
    public ResponseEntity<List<ModuleWithActionsDto>> getModulesWithActions() {
        log.info("📋 [RoleController] Recibida solicitud para obtener módulos con acciones disponibles");
        try {
            List<ModuleWithActionsDto> modulesWithActions = roleService.getModulesWithActions();
            log.info("✅ [RoleController] Devueltos {} módulos con acciones", modulesWithActions.size());
            return ResponseEntity.ok(modulesWithActions);
        } catch (Exception e) {
            log.error("❌ [RoleController] Error obteniendo módulos con acciones: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}