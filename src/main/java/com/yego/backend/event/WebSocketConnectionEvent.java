package com.yego.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Evento de conexión WebSocket
 * Equivalente a los eventos emitidos en handleConnection de NestJS
 */
@Getter
public class WebSocketConnectionEvent extends ApplicationEvent {
    private final String sessionId;
    private final Long userId;
    private final String socketId;
    
    public WebSocketConnectionEvent(Object source, String sessionId, Long userId, String socketId) {
        super(source);
        this.sessionId = sessionId;
        this.userId = userId;
        this.socketId = socketId;
    }
}

