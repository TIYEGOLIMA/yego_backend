package com.yego.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración de WebSocket para el Backend Principal
 * 
 * Hub de comunicación en tiempo real para todos los microfrontends:
 * - /topic/ticketera/* - Eventos del microfrontend Ticketera
 * - /topic/okr/*       - Eventos del microfrontend OKR  
 * - /topic/marketing/* - Eventos del microfrontend Marketing
 * - /topic/system/*    - Eventos globales del sistema
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilitar broker simple para tópicos
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefijo para mensajes destinados a métodos anotados con @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefijo para mensajes de usuario específico
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint principal para WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
                
        // Endpoint específico para cada microfrontend
        registry.addEndpoint("/ws/ticketera")
                .setAllowedOriginPatterns("*")
                .withSockJS();
                
        registry.addEndpoint("/ws/okr")
                .setAllowedOriginPatterns("*")
                .withSockJS();
                
        registry.addEndpoint("/ws/marketing")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}

