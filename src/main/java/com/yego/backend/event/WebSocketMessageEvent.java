package com.yego.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Evento genérico para mensajes WebSocket
 * Equivalente a los métodos emitToUser, emitToSession, emitToAll de NestJS
 */
@Getter
public class WebSocketMessageEvent extends ApplicationEvent {
    private final String eventType;
    private final Object data;
    private final Long userId;
    private final String sessionId;
    private final boolean broadcastToAll;
    
    // Constructor para mensaje a usuario específico
    public WebSocketMessageEvent(Object source, String eventType, Object data, Long userId) {
        super(source);
        this.eventType = eventType;
        this.data = data;
        this.userId = userId;
        this.sessionId = null;
        this.broadcastToAll = false;
    }
    
    // Constructor para mensaje a sesión específica
    public WebSocketMessageEvent(Object source, String eventType, Object data, String sessionId) {
        super(source);
        this.eventType = eventType;
        this.data = data;
        this.userId = null;
        this.sessionId = sessionId;
        this.broadcastToAll = false;
    }
    
    // Constructor para broadcast a todos
    public WebSocketMessageEvent(Object source, String eventType, Object data) {
        super(source);
        this.eventType = eventType;
        this.data = data;
        this.userId = null;
        this.sessionId = null;
        this.broadcastToAll = true;
    }
}

