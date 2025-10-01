package com.yego.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controlador WebSocket centralizado - Solo endpoints esenciales
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {
    
    /**
     * Endpoint para mensajes del sistema
     */
    @MessageMapping("/system")
    @SendTo("/topic/system")
    public Map<String, Object> handleSystemMessage(@Payload Map<String, Object> message, 
                                                   SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        String username = auth != null ? auth.getName() : "anonymous";
        
        log.info("📨 [WebSocket] Sistema desde {}: {}", username, message);
        
        message.put("timestamp", LocalDateTime.now().toString());
        message.put("from", username);
        
        return message;
    }
    
    /**
     * Endpoint para eventos de Ticketera
     */
    @MessageMapping("/ticketera")
    @SendTo("/topic/ticketera")
    public Map<String, Object> handleTicketeraEvent(@Payload Map<String, Object> event, 
                                                    SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        String username = auth != null ? auth.getName() : "anonymous";
        
        log.info("🎫 [WebSocket] Ticketera desde {}: {}", username, event);
        
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("from", username);
        
        return event;
    }
}
