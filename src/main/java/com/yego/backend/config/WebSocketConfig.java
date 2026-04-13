package com.yego.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de WebSocket
 * Usa WebSocket nativo siempre (sin SockJS)
 * - Desarrollo: ws:// (sin SSL)
 * - Producción: wss:// (con SSL)
 * 
 * Hub de comunicación en tiempo real para todos los microfrontends:
 * - /topic/ticketera/* - Eventos del microfrontend Ticketera
 * - /topic/marketing/* - Eventos del microfrontend Marketingamar
 * - /topic/system/*    - Eventos globales del sistema
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    
    @Autowired(required = false)
    private TaskScheduler taskScheduler;
    
    // Orígenes permitidos para WebSocket (debe coincidir con SecurityConfig)
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:3030",
        "http://localhost:5173",
        "http://localhost:5174",
        "https://integral.yego.pro",
        "https://api-int.yego.pro",
        "https://neto.yego.pro",
        "https://siscoca.yego.pro",
        "https://ct4.yego.pro",
        "http://5.161.86.63",
        "http://5.161.86.63:5173",
        "http://5.161.86.63:3030"
    );

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilitar broker simple para tópicos con heartbeat
        // Heartbeat cada 10 segundos para detectar conexiones muertas
        var brokerRegistration = config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000}); // Enviar cada 10s, esperar respuesta cada 10s
        
        // Configurar TaskScheduler explícitamente si está disponible
        if (taskScheduler != null) {
            brokerRegistration.setTaskScheduler(taskScheduler);
            log.info("✅ [WebSocket] Broker configurado con heartbeat cada 10 segundos");
        } else {
            log.warn("⚠️ [WebSocket] TaskScheduler no disponible, heartbeat puede no funcionar correctamente");
        }
        
        // Prefijo para mensajes destinados a métodos anotados con @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefijo para mensajes de usuario específico
        config.setUserDestinationPrefix("/user");
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
        
        // Siempre usar WebSocket nativo (sin SockJS)
        // La diferencia entre desarrollo y producción es solo el protocolo:
        // - Desarrollo: ws://localhost:3030/ws
        // - Producción: wss://api-int.yego.pro/ws
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);
        
        log.info("✅ [WebSocket] Endpoint /ws configurado (WebSocket nativo) - Orígenes permitidos: {}", String.join(", ", ALLOWED_ORIGINS));
    }
}

