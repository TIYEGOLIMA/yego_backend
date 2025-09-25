package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.service.yego_principal.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para sesiones del sistema YEGO Principal
 * Equivalente a SessionsController de NestJS
 */
@Slf4j
@RestController
@RequestMapping("/api/yego-principal/sessions")
@RequiredArgsConstructor
public class SessionController {
    
    private final SessionService sessionService;
    
    /**
     * Crear nueva sesión
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> create(@Valid @RequestBody CreateSessionDto createSessionDto, 
                                   Authentication authentication, 
                                   HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }
            
            SessionResponseDto session = sessionService.create(createSessionDto, userId, request);
            log.info("✅ Sesión YEGO Principal creada: {}", session.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
            
        } catch (Exception e) {
            log.error("Error creando sesión YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener todas las sesiones activas
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> findAll(Authentication authentication) {
        try {
            // Los admins ven todas las sesiones, los supervisores solo las de su equipo
            Long userId = hasRole(authentication, "ADMIN") ? null : getCurrentUserId(authentication);
            
            List<SessionResponseDto> sessions = sessionService.findAll(userId);
            return ResponseEntity.ok(sessions);
            
        } catch (Exception e) {
            log.error("Error obteniendo sesiones YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener estadísticas de sesiones
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStats() {
        try {
            SessionStatsDto stats = sessionService.getSessionStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de sesiones YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener estadísticas de WebSocket
     */
    @GetMapping("/websocket/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getWebSocketStats() {
        try {
            ConnectionStatsDto stats = sessionService.getWebSocketStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas WebSocket YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener sesiones activas de WebSocket
     */
    @GetMapping("/websocket/sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getWebSocketSessions() {
        try {
            List<SessionDataDto> sessions = sessionService.getWebSocketSessions();
            return ResponseEntity.ok(sessions);
            
        } catch (Exception e) {
            log.error("Error obteniendo sesiones WebSocket YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener historial de conexiones
     */
    @GetMapping("/connection-logs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> getConnectionLogs(
            @RequestParam(defaultValue = "30") Integer days,
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String roleName) {
        
        try {
            log.info("🔍 Iniciando getConnectionLogs YEGO Principal - days: {}, limit: {}, userId: {}, roleName: {}", 
                    days, limit, userId, roleName);
            
            List<ConnectionLogResponseDto> logs = sessionService.getConnectionLogs(days, limit, userId, roleName);
            
            log.info("✅ getConnectionLogs YEGO Principal completado exitosamente: {} registros", logs.size());
            return ResponseEntity.ok(logs);
            
        } catch (Exception e) {
            log.error("❌ Error en getConnectionLogs YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener cantidad de sesiones activas
     */
    @GetMapping("/count/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getActiveSessionsCount(Authentication authentication) {
        try {
            Long userId = hasRole(authentication, "ADMIN") ? null : getCurrentUserId(authentication);
            Long count = sessionService.getActiveSessionsCount(userId);
            
            return ResponseEntity.ok(Map.of("count", count));
            
        } catch (Exception e) {
            log.error("Error obteniendo conteo de sesiones YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener sesión por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        try {
            SessionResponseDto session = sessionService.findOne(id);
            return ResponseEntity.ok(session);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo sesión YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Cerrar sesión específica
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        try {
            sessionService.deactivate(id);
            log.info("🚪 Sesión YEGO Principal cerrada: {}", id);
            
            return ResponseEntity.noContent().build();
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error cerrando sesión YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Cerrar todas las sesiones de un usuario
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateByUserId(@PathVariable Long userId, 
                                               @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : "Sesión cerrada por administrador";
            sessionService.deactivateByUserId(userId, reason);
            
            log.info("🚪 Sesiones YEGO Principal cerradas para usuario: {}", userId);
            
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Error cerrando sesiones del usuario YEGO Principal {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Limpiar sesiones expiradas
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cleanupExpiredSessions() {
        try {
            Integer count = sessionService.cleanupExpiredSessions();
            
            log.info("🧹 Sesiones expiradas limpiadas YEGO Principal: {}", count);
            
            return ResponseEntity.ok(Map.of(
                "message", count + " sesiones expiradas limpiadas",
                "count", count
            ));
            
        } catch (Exception e) {
            log.error("Error limpiando sesiones expiradas YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Forzar cierre de sesión
     */
    @PostMapping("/{id}/force-logout")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<?> forceLogout(@PathVariable Long id, Authentication authentication) {
        try {
            Long adminUserId = getCurrentUserId(authentication);
            if (adminUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Usuario no autenticado"));
            }
            
            sessionService.forceLogout(id, adminUserId);
            
            log.info("🚪 Sesión YEGO Principal forzada a cerrar: {} por admin: {}", id, adminUserId);
            
            return ResponseEntity.noContent().build();
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error forzando cierre de sesión YEGO Principal {}: {}", id, e.getMessage());
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
    
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
