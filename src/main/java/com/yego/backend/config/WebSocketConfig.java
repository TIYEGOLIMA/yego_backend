package com.yego.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de WebSocket para el Backend Principal
 * 
 * Hub de comunicación en tiempo real para todos los microfrontends:
 * - /topic/ticketera/* - Eventos del microfrontend Ticketera
 * - /topic/okr/*       - Eventos del microfrontend OKR  
 * - /topic/marketing/* - Eventos del microfrontend Marketingamar
 * - /topic/system/*    - Eventos globales del sistema
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    // Orígenes permitidos para WebSocket (debe coincidir con SecurityConfig)
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:3030",
        "http://localhost:5173",
        "http://localhost:5174",
        "https://integral.yego.pro",
        "https://api-int.yego.pro",
        "https://neto.yego.pro",
        "https://siscoca.yego.pro"
    );

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilitar broker simple para tópicos con heartbeat
        // Heartbeat cada 10 segundos para detectar conexiones muertas
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000}); // Enviar cada 10s, esperar respuesta cada 10s
        
        // Prefijo para mensajes destinados a métodos anotados con @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefijo para mensajes de usuario específico
        config.setUserDestinationPrefix("/user");
        
        log.info("✅ [WebSocket] Broker configurado con heartbeat cada 10 segundos");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Registrar interceptor de autenticación
        registration.interceptors(webSocketAuthInterceptor);
        
        // Limitar threads del canal de entrada para evitar saturación
        registration.taskExecutor().corePoolSize(4);
        registration.taskExecutor().maxPoolSize(8);
        registration.taskExecutor().queueCapacity(100);
    }
    
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Limitar threads del canal de salida
        registration.taskExecutor().corePoolSize(4);
        registration.taskExecutor().maxPoolSize(8);
        registration.taskExecutor().queueCapacity(100);
    }
    

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = ALLOWED_ORIGINS.toArray(new String[0]);
        
        // En desarrollo: usar SockJS (con fallback a polling)
        // En producción: usar solo WebSocket nativo (sin polling)
        boolean isProduction = activeProfile != null && (activeProfile.contains("prod") || activeProfile.contains("production"));
        
        if (isProduction) {
            // Producción: solo WebSocket nativo (sin SockJS)
            registry.addEndpoint("/ws")
                    .setAllowedOrigins(allowedOrigins);
        } else {
            // Desarrollo: SockJS con fallback a polling
            registry.addEndpoint("/ws")
                    .setAllowedOrigins(allowedOrigins)
                    .withSockJS();
        }
    }
}

