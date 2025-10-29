package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.CreatePermissionDto;
import com.yego.backend.entity.yego_principal.api.request.UpdatePermissionDto;
import com.yego.backend.entity.yego_principal.api.response.PermissionResponseDto;
import com.yego.backend.service.yego_principal.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;
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
    public ResponseEntity<PermissionResponseDto> create(@Valid @RequestBody CreatePermissionDto createPermissionDto) {
        log.info("📋 [PermissionController] Recibida solicitud para crear permiso: {}", createPermissionDto.getName());
        try {
            PermissionResponseDto permission = permissionService.create(createPermissionDto);
            log.info("✅ [PermissionController] Permiso creado: {}", permission.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(permission);
        } catch (IllegalStateException e) {
            log.error("❌ [PermissionController] Error creando permiso: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("❌ [PermissionController] Error creando permiso: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtener permiso por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponseDto> findOne(@PathVariable Long id) {
        log.info("📋 [PermissionController] Recibida solicitud para obtener permiso por ID: {}", id);
        try {
            PermissionResponseDto permission = permissionService.findOne(id);
            log.info("✅ [PermissionController] Permiso encontrado: {}", permission.getName());
            return ResponseEntity.ok(permission);
        } catch (EntityNotFoundException e) {
            log.error("❌ [PermissionController] Permiso no encontrado con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("❌ [PermissionController] Error obteniendo permiso: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtener todos los permisos activos
     */
    @GetMapping("/find-all-active")
    public ResponseEntity<List<PermissionResponseDto>> findAllActive() {
        List<PermissionResponseDto> permissions = permissionService.findAllActive();
        return ResponseEntity.ok(permissions);
    }
    
    /**
     * Obtener todos los permisos
     */
    @GetMapping("/find-all")
    public ResponseEntity<List<PermissionResponseDto>> findAll() {
        List<PermissionResponseDto> permissions = permissionService.findAll();
        return ResponseEntity.ok(permissions);
    }
    
    /**
     * Obtener permisos por módulo
     */
    @GetMapping("/module/{module}")
    public ResponseEntity<List<PermissionResponseDto>> findByModule(@PathVariable String module) {
        List<PermissionResponseDto> permissions = permissionService.findByModule(module);
        return ResponseEntity.ok(permissions);
    }
    
    /**
     * Actualizar permiso
     */
    @PutMapping("/{id}")
    public ResponseEntity<PermissionResponseDto> update(@PathVariable Long id, 
                                                        @Valid @RequestBody UpdatePermissionDto updatePermissionDto) {
        log.info("📋 [PermissionController] Recibida solicitud para actualizar permiso con ID: {}", id);
        try {
            PermissionResponseDto permission = permissionService.update(id, updatePermissionDto);
            log.info("✅ [PermissionController] Permiso actualizado: {}", permission.getName());
            return ResponseEntity.ok(permission);
        } catch (EntityNotFoundException e) {
            log.error("❌ [PermissionController] Permiso no encontrado con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.error("❌ [PermissionController] Error actualizando permiso: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("❌ [PermissionController] Error actualizando permiso: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Eliminar permiso (soft delete - desactivar)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        log.info("📋 [PermissionController] Recibida solicitud para eliminar permiso con ID: {}", id);
        try {
            permissionService.remove(id);
            log.info("✅ [PermissionController] Permiso eliminado (desactivado) con ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.error("❌ [PermissionController] Permiso no encontrado con ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("❌ [PermissionController] Error eliminando permiso: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}