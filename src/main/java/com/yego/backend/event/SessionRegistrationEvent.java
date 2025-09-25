package com.yego.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Evento de registro de sesión WebSocket
 * Equivalente al evento emitido en handleRegisterSession de NestJS
 */
@Getter
public class SessionRegistrationEvent extends ApplicationEvent {
    private final String sessionId;
    private final Long userId;
    private final String socketId;
    
    public SessionRegistrationEvent(Object source, String sessionId, Long userId, String socketId) {
        super(source);
        this.sessionId = sessionId;
        this.userId = userId;
        this.socketId = socketId;
    }
}
