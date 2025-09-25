package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.service.yego_principal.RoleService;
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
 * Controlador REST para roles del sistema YEGO Principal
 * Equivalente a RolesController de NestJS
 */
@Slf4j
@RestController
@RequestMapping("/api/yego-principal/roles")
@RequiredArgsConstructor
public class RoleController {
    
    private final RoleService roleService;
    
    /**
     * Crear nuevo rol
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateRoleDto createRoleDto) {
        try {
            RoleResponseDto role = roleService.create(createRoleDto);
            log.info("✅ Rol YEGO Principal creado via API: {}", role.getName());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Rol creado exitosamente",
                "role", role
            ));
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creando rol YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener todos los roles
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> findAll(@RequestParam(required = false) String includeUsers) {
        try {
            List<RoleResponseDto> roles = roleService.findAll();
            
            if ("true".equals(includeUsers)) {
                // Agregar conteo de usuarios por rol
                List<RoleWithUsersDto> rolesWithUsers = roles.stream()
                        .map(role -> {
                            Long userCount = roleService.getUserCountByRole(role.getName());
                            return RoleWithUsersDto.builder()
                                    .id(role.getId())
                                    .name(role.getName())
                                    .description(role.getDescription())
                                    .permissions(role.getPermissions())
                                    .activo(role.getActivo())
                                    .createdAt(role.getCreatedAt())
                                    .updatedAt(role.getUpdatedAt())
                                    .userCount(userCount)
                                    .build();
                        })
                        .toList();
                
                return ResponseEntity.ok(rolesWithUsers);
            }
            
            return ResponseEntity.ok(roles);
            
        } catch (Exception e) {
            log.error("Error obteniendo roles YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener roles por defecto
     */
    @GetMapping("/default")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDefaultRoles() {
        try {
            List<RoleResponseDto> defaultRoles = roleService.getDefaultRoles();
            return ResponseEntity.ok(defaultRoles);
            
        } catch (Exception e) {
            log.error("Error obteniendo roles por defecto YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener rol por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        try {
            RoleResponseDto role = roleService.findOne(id);
            Long userCount = roleService.getUserCountByRole(role.getName());
            
            return ResponseEntity.ok(Map.of(
                "role", role,
                "user_count", userCount
            ));
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo rol YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Actualizar rol
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, 
                                   @Valid @RequestBody UpdateRoleDto updateRoleDto) {
        try {
            RoleResponseDto role = roleService.update(id, updateRoleDto);
            log.info("✅ Rol YEGO Principal actualizado via API: {}", role.getName());
            
            return ResponseEntity.ok(Map.of(
                "message", "Rol actualizado exitosamente",
                "role", role
            ));
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error actualizando rol YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Eliminar rol
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        try {
            roleService.remove(id);
            log.info("🗑️ Rol YEGO Principal eliminado via API: ID {}", id);
            
            return ResponseEntity.ok(Map.of("message", "Rol eliminado exitosamente"));
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error eliminando rol YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Inicializar roles por defecto
     */
    @PostMapping("/initialize")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<?> initializeDefaultRoles() {
        try {
            roleService.initializeDefaultRoles();
            log.info("⚙️ Roles por defecto YEGO Principal inicializados via API");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Roles por defecto inicializados exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error inicializando roles por defecto YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
}
