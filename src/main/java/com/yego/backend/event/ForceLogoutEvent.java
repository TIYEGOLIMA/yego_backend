package com.yego.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Evento para forzar logout de usuario
 * Equivalente al método forceLogout en WebSocketGateway de NestJS
 */
@Getter
public class ForceLogoutEvent extends ApplicationEvent {
    private final Long userId;
    private final String reason;
    
    public ForceLogoutEvent(Object source, Long userId, String reason) {
        super(source);
        this.userId = userId;
        this.reason = reason;
    }
}

