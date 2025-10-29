package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para sesiones del sistema YEGO Principal
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    
    private final SessionService sessionService;
    
    /**
     * Obtener sesiones del usuario actual
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUserSessions(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<SessionResponseDto> sessions = sessionService.findAll(userId);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Obtener sesión por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        SessionResponseDto session = sessionService.findOne(id);
        return ResponseEntity.ok(session);
    }
    
    /**
     * Cerrar sesión por ID
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> closeSession(@PathVariable Long sessionId) {
        sessionService.deactivate(sessionId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Cerrar todas las sesiones de un usuario
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> closeUserSessions(@PathVariable Long userId) {
        sessionService.deactivateByUserId(userId, "Cerrado por administrador");
        return ResponseEntity.ok().build();
    }
    
    /**
     * Obtener estadísticas de sesiones
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSessionStats() {
        SessionStatsDto stats = sessionService.getSessionStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener estadísticas de WebSocket
     */
    @GetMapping("/websocket/stats")
    public ResponseEntity<?> getWebSocketStats() {
        ConnectionStatsDto stats = sessionService.getWebSocketStats();
        return ResponseEntity.ok(stats);
    }
}