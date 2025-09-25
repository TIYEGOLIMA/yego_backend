package com.yego.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Evento para cerrar sesión específica
 * Equivalente al método closeSession en WebSocketGateway de NestJS
 */
@Getter
public class SessionClosedEvent extends ApplicationEvent {
    private final String sessionId;
    private final String reason;
    
    public SessionClosedEvent(Object source, String sessionId, String reason) {
        super(source);
        this.sessionId = sessionId;
        this.reason = reason;
    }
}
