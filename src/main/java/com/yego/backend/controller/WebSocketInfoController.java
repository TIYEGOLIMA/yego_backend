package com.yego.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador para manejar peticiones a /ws/info (SockJS)
 * En producción, SockJS no está habilitado, solo WebSocket nativo
 */
@Slf4j
@RestController
@RequestMapping("/ws")
public class WebSocketInfoController {
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    /**
     * Maneja peticiones a /ws/info (usado por SockJS)
     * En producción devuelve un mensaje claro indicando que debe usar WebSocket nativo
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> wsInfo() {
        boolean isProduction = activeProfile != null && (activeProfile.contains("prod") || activeProfile.contains("production"));
        
        if (isProduction) {
            log.debug("⚠️ [WebSocket] Petición a /ws/info rechazada - SockJS no disponible en producción");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "SockJS no está disponible en producción",
                "message", "Use WebSocket nativo: wss://api-int.yego.pro/ws",
                "websocket_native", "wss://api-int.yego.pro/ws",
                "sockjs_enabled", false
            ));
        }
        
        // En desarrollo, SockJS está habilitado, pero este endpoint no debería ser llamado directamente
        // SockJS maneja esto internamente
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "error", "Endpoint no disponible",
            "message", "Use el endpoint WebSocket configurado"
        ));
    }
}

