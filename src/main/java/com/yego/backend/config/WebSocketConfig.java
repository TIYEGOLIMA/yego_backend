package com.yego.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
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
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

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
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Registrar interceptor de autenticación
        registration.interceptors(webSocketAuthInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint principal para WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174", "https://integral.yego.pro", "https://api-int.yego.pro", "https://neto.yego.pro")
                .withSockJS();

        // Endpoint específico para cada microfrontend
        registry.addEndpoint("/ws/ticketera")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174", "https://integral.yego.pro", "https://api-int.yego.pro", "https://neto.yego.pro")
                .withSockJS();

        registry.addEndpoint("/ws/okr")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174", "https://integral.yego.pro", "https://api-int.yego.pro", "https://neto.yego.pro")
                .withSockJS();

        registry.addEndpoint("/ws/marketing")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174", "https://integral.yego.pro", "https://api-int.yego.pro", "https://neto.yego.pro")
                .withSockJS();
    }
}

