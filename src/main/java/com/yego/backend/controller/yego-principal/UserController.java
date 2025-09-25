package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.service.yego_principal.UserService;
import com.yego.backend.service.yego_principal.DniValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * Controlador REST para usuarios del sistema YEGO Principal
 * Equivalente a UsersController de NestJS
 */
@Slf4j
@RestController
@RequestMapping("/api/yego-principal/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final DniValidationService dniValidationService;
    
    /**
     * Obtener perfil del usuario actual
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }
            
            UserResponseDto user = userService.findOne(userId);
            return ResponseEntity.ok(user);
            
        } catch (Exception e) {
            log.error("Error obteniendo perfil YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Crear nuevo usuario
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserDto createUserDto) {
        try {
            UserResponseDto user = userService.create(createUserDto);
            log.info("✅ Usuario YEGO Principal creado: {}", user.getUsername());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creando usuario YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Ejemplo de creación de usuario
     */
    @PostMapping("/example")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> createExample(@Valid @RequestBody CreateUserExampleDto createUserDto) {
        try {
            UserResponseDto user = userService.createFromExample(createUserDto);
            log.info("✅ Usuario ejemplo YEGO Principal creado: {}", user.getUsername());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creando usuario ejemplo YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Listar usuarios con paginación y filtros
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> findAll(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String activo) {
        
        try {
            Boolean active = null;
            if ("true".equals(activo)) {
                active = true;
            } else if ("false".equals(activo)) {
                active = false;
            }
            // Si activo es "all" o null, active permanece null
            
            UserPageDto result = userService.findAll(page, limit, search, active);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error obteniendo usuarios YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener usuario por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        try {
            UserResponseDto user = userService.findOne(id);
            return ResponseEntity.ok(user);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo usuario YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Actualizar usuario
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, 
                                   @Valid @RequestBody UpdateUserDto updateUserDto) {
        try {
            UserResponseDto user = userService.update(id, updateUserDto);
            log.info("✅ Usuario YEGO Principal actualizado: {}", user.getUsername());
            
            return ResponseEntity.ok(user);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error actualizando usuario YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Eliminar usuario (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        try {
            userService.remove(id);
            log.info("🗑️ Usuario YEGO Principal eliminado: {}", id);
            
            return ResponseEntity.ok(Map.of("message", "Usuario eliminado exitosamente"));
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error eliminando usuario YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Activar usuario
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        try {
            UserResponseDto user = userService.activate(id);
            log.info("✅ Usuario YEGO Principal activado: {}", user.getUsername());
            
            return ResponseEntity.ok(user);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error activando usuario YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Desactivar usuario
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        try {
            UserResponseDto user = userService.deactivate(id);
            log.info("❌ Usuario YEGO Principal desactivado: {}", user.getUsername());
            
            return ResponseEntity.ok(user);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error desactivando usuario YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Cambiar contraseña de usuario
     */
    @PatchMapping("/{id}/change-password")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> changePassword(@PathVariable Long id, 
                                           @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "La nueva contraseña es requerida"));
            }
            
            userService.changePassword(id, newPassword);
            log.info("🔑 Contraseña cambiada para usuario YEGO Principal: {}", id);
            
            return ResponseEntity.ok(Map.of("message", "Contraseña cambiada exitosamente"));
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error cambiando contraseña usuario YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Validar DNI con API externa
     */
    @GetMapping("/validate-dni/{dni}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> validateDni(@PathVariable String dni) {
        try {
            DniValidationDto result = dniValidationService.validateDni(dni);
            
            if (result.getSuccess()) {
                log.info("✅ DNI validado exitosamente en YEGO Principal: {}", dni);
            } else {
                log.warn("❌ DNI no encontrado en YEGO Principal: {}", dni);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error validando DNI en YEGO Principal {}: {}", dni, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                return Long.parseLong(authentication.getName());
            }
            return null;
        } catch (NumberFormatException e) {
            log.warn("Error obteniendo userId del authentication: {}", e.getMessage());
            return null;
        }
    }
}
