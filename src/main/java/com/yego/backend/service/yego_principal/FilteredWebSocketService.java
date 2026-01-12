package com.yego.backend.service.yego_principal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Servicio que envuelve SimpMessagingTemplate para enviar mensajes WebSocket
 * Las suscripciones ya están filtradas por acceso en WebSocketAuthInterceptor
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilteredWebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionService webSocketSessionService;
    
    /**
     * Envía un mensaje a un topic
     * Para /topic/modulos-atencion, envía a TODAS las sesiones con acceso al módulo "tickets"
     * Para otros topics, solo envía si hay sesiones suscritas (ya filtradas por acceso)
     * 
     * @param destination Topic de destino (ej: /topic/pro-ops/kpis)
     * @param payload Mensaje a enviar
     */
    public void convertAndSend(String destination, Object payload) {
        if (destination == null) {
            return;
        }
        
        // Normalizar el topic
        String normalizedTopic = destination.startsWith("/topic/") 
            ? destination.substring(7) 
            : destination.startsWith("topic/") 
                ? destination.substring(6) 
                : destination;
        
        // Topics del sistema siempre se envían
        if (normalizedTopic.startsWith("system") || normalizedTopic.startsWith("user/")) {
            messagingTemplate.convertAndSend(destination, payload);
            log.info("📤 [FilteredWebSocket] Mensaje enviado a topic de sistema: {}", destination);
            return;
        }
        
        // Para /topic/modulos-atencion, enviar a TODAS las sesiones con acceso al módulo "tickets"
        // Esto es necesario porque afecta a todos los usuarios en sesión (asignar/liberar módulos)
        if (normalizedTopic.equals("modulos-atencion")) {
            Set<String> sessionsWithAccess = webSocketSessionService.getSessionsWithModuleAccess("tickets");
            if (sessionsWithAccess.isEmpty()) {
                log.debug("⏭️ [FilteredWebSocket] No hay sesiones con acceso al módulo 'tickets' - omitiendo envío");
                return;
            }
            
            // Enviar al topic - esto llegará a todas las sesiones suscritas
            messagingTemplate.convertAndSend(destination, payload);
            
            // También enviar al topic del sistema con un tipo específico para asegurar que todos lo reciban
            // El frontend puede escuchar /topic/system y filtrar por type: "MODULOS_ACTUALIZADOS"
            java.util.Map<String, Object> systemPayload = new java.util.HashMap<>();
            if (payload instanceof java.util.Map) {
                systemPayload.putAll((java.util.Map<String, Object>) payload);
            } else {
                systemPayload.put("data", payload);
            }
            messagingTemplate.convertAndSend("/topic/system", systemPayload);
            
            log.info("📤 [FilteredWebSocket] Mensaje enviado a topic {} y /topic/system - {} sesiones con acceso al módulo 'tickets': {}", 
                destination, sessionsWithAccess.size(), sessionsWithAccess);
            return;
        }
        
        // Para topics de garantizado, también enviar a /topic/system
        if (normalizedTopic.startsWith("garantizado/") || normalizedTopic.startsWith("garantizado")) {
            Set<String> sessionsWithAccess = webSocketSessionService.getSessionsWithModuleAccess("garantizado");
            if (sessionsWithAccess.isEmpty()) {
                log.debug("⏭️ [FilteredWebSocket] No hay sesiones con acceso al módulo 'garantizado' - omitiendo envío");
                return;
            }
            
            // Enviar al topic específico
            messagingTemplate.convertAndSend(destination, payload);
            
            // También enviar al topic del sistema
            java.util.Map<String, Object> systemPayload = new java.util.HashMap<>();
            if (payload instanceof java.util.Map) {
                systemPayload.putAll((java.util.Map<String, Object>) payload);
            } else {
                systemPayload.put("data", payload);
            }
            messagingTemplate.convertAndSend("/topic/system", systemPayload);
            
            log.info("📤 [FilteredWebSocket] Mensaje enviado a topic {} y /topic/system - {} sesiones con acceso al módulo 'garantizado': {}", 
                destination, sessionsWithAccess.size(), sessionsWithAccess);
            return;
        }
        
        // Para topics de pro-ops, también enviar a /topic/system
        if (normalizedTopic.startsWith("pro-ops/") || normalizedTopic.startsWith("pro-ops")) {
            Set<String> sessionsWithAccess = webSocketSessionService.getSessionsWithModuleAccess("pro-ops");
            if (sessionsWithAccess.isEmpty()) {
                log.debug("⏭️ [FilteredWebSocket] No hay sesiones con acceso al módulo 'pro-ops' - omitiendo envío");
                return;
            }
            
            // Enviar al topic específico
            messagingTemplate.convertAndSend(destination, payload);
            
            // También enviar al topic del sistema
            java.util.Map<String, Object> systemPayload = new java.util.HashMap<>();
            if (payload instanceof java.util.Map) {
                systemPayload.putAll((java.util.Map<String, Object>) payload);
            } else {
                systemPayload.put("data", payload);
            }
            messagingTemplate.convertAndSend("/topic/system", systemPayload);
            
            log.info("📤 [FilteredWebSocket] Mensaje enviado a topic {} y /topic/system - {} sesiones con acceso al módulo 'pro-ops': {}", 
                destination, sessionsWithAccess.size(), sessionsWithAccess);
            return;
        }
        
        // Para otros topics, verificar si hay sesiones suscritas (ya filtradas por acceso)
        Set<String> subscribedSessions = webSocketSessionService.getSessionsSubscribedTo(destination);
        
        if (subscribedSessions.isEmpty()) {
            log.warn("🚫 [FilteredWebSocket] No hay sesiones suscritas al topic: {} - NO ENVIANDO MENSAJE", destination);
            return;
        }
        
        // Enviar mensaje - solo usuarios con acceso están suscritos
        // Con SimpleBroker, convertAndSend envía a todas las sesiones suscritas al topic
        messagingTemplate.convertAndSend(destination, payload);
        log.info("📤 [FilteredWebSocket] Mensaje enviado a topic {} - {} sesiones suscritas: {}", 
            destination, subscribedSessions.size(), subscribedSessions);
    }
}

