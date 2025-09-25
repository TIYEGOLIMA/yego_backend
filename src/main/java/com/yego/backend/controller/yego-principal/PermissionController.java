package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.service.yego_principal.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para permisos del sistema YEGO Principal
 * Equivalente a PermissionsController de NestJS
 */
@Slf4j
@RestController
@RequestMapping("/api/yego-principal/permissions")
@RequiredArgsConstructor
public class PermissionController {
    
    private final PermissionService permissionService;
    
    /**
     * Crear nuevo permiso
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreatePermissionDto createPermissionDto) {
        try {
            PermissionResponseDto permission = permissionService.create(createPermissionDto);
            log.info("✅ Permiso YEGO Principal creado via API: {}", permission.getName());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(permission);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creando permiso YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener todos los permisos
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findAll(@RequestParam(required = false) String module) {
        try {
            List<PermissionResponseDto> permissions;
            
            if (module != null && !module.trim().isEmpty()) {
                permissions = permissionService.findByModule(module);
            } else {
                permissions = permissionService.findAll();
            }
            
            return ResponseEntity.ok(permissions);
            
        } catch (Exception e) {
            log.error("Error obteniendo permisos YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener permiso por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        try {
            PermissionResponseDto permission = permissionService.findOne(id);
            return ResponseEntity.ok(permission);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo permiso YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Actualizar permiso
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, 
                                   @Valid @RequestBody UpdatePermissionDto updatePermissionDto) {
        try {
            PermissionResponseDto permission = permissionService.update(id, updatePermissionDto);
            log.info("✅ Permiso YEGO Principal actualizado via API: {}", permission.getName());
            
            return ResponseEntity.ok(permission);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error actualizando permiso YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Eliminar permiso (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        try {
            permissionService.remove(id);
            log.info("🗑️ Permiso YEGO Principal desactivado via API: ID {}", id);
            
            return ResponseEntity.ok(Map.of("message", "Permiso desactivado exitosamente"));
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error desactivando permiso YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Inicializar permisos por defecto
     */
    @PostMapping("/initialize")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> initializeDefaultPermissions() {
        try {
            permissionService.initializeDefaultPermissions();
            log.info("⚙️ Permisos por defecto YEGO Principal inicializados via API");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Permisos por defecto inicializados exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error inicializando permisos por defecto YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Verificar permiso de usuario
     */
    @GetMapping("/check/{userId}/{permissionName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> checkPermission(@PathVariable Long userId, 
                                           @PathVariable String permissionName) {
        try {
            boolean hasPermission = permissionService.checkPermission(userId, permissionName);
            
            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "permission", permissionName,
                "hasPermission", hasPermission
            ));
            
        } catch (Exception e) {
            log.error("Error verificando permiso YEGO Principal {} para usuario {}: {}", 
                    permissionName, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
}
