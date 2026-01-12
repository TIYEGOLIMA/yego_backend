package com.yego.backend.config;

import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import com.yego.backend.service.yego_principal.ModuleService;
import com.yego.backend.service.yego_principal.WebSocketModuleMappingService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Interceptor de autenticación para WebSocket
 * Valida tokens JWT en las conexiones WebSocket
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    private final ModuleService moduleService;
    private final WebSocketSessionService webSocketSessionService;
    private final WebSocketModuleMappingService webSocketModuleMappingService;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor == null) {
            return message;
        }
        
        StompCommand command = accessor.getCommand();
        
        // Manejar conexión (CONNECT)
        if (StompCommand.CONNECT.equals(command)) {
            return handleConnect(accessor, message);
        }
        
        // Manejar suscripción (SUBSCRIBE)
        if (StompCommand.SUBSCRIBE.equals(command)) {
            return handleSubscribe(accessor, message);
        }
        
        // Manejar desconexión (DISCONNECT)
        if (StompCommand.DISCONNECT.equals(command)) {
            return handleDisconnect(accessor, message);
        }
        
        return message;
    }
    
    private Message<?> handleConnect(StompHeaderAccessor accessor, Message<?> message) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("⚠️ [WebSocket] Token de autenticación requerido");
            throw new org.springframework.messaging.MessageDeliveryException("Token de autenticación requerido");
        }
        
                String token = authHeader.substring(7);
                
                try {
            if (!validateToken(token)) {
                log.warn("⚠️ [WebSocket] Token JWT inválido");
                throw new org.springframework.messaging.MessageDeliveryException("Token JWT inválido");
            }
            
                        Claims claims = getClaimsFromToken(token);
                        String username = claims.get("username", String.class);
                        if (username == null) {
                            username = claims.getSubject();
                        }
                        String role = claims.get("role", String.class);
                        Integer userIdInt = claims.get("userId", Integer.class);
                        String userId = userIdInt != null ? userIdInt.toString() : username;
                        
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                userId, 
                                null, 
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);
                        
            // Guardar módulos del usuario en la sesión
            Long userIdLong = userIdInt != null ? userIdInt : (userId != null && userId.matches("\\d+") ? Long.parseLong(userId) : null);
            if (userIdLong != null) {
                try {
                    List<ModuleResponse> userModules = moduleService.obtenerModulosPorUsuario(userIdLong);
                    String sessionId = accessor.getSessionId();
                    if (sessionId != null) {
                        webSocketSessionService.saveUserModules(sessionId, userModules, userId);
                        log.info("✅ [WebSocket] Usuario autenticado: {} (ID: {}) con rol: {} - Módulos: {}", 
                            username, userId, role, userModules.size());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [WebSocket] Error obteniendo módulos del usuario {}: {}", userId, e.getMessage());
                }
            }
            
        } catch (org.springframework.messaging.MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.warn("⚠️ [WebSocket] Error validando token: {}", e.getMessage());
            throw new org.springframework.messaging.MessageDeliveryException("Error validando token: " + e.getMessage());
        }
        
        return message;
    }
    
    private Message<?> handleSubscribe(StompHeaderAccessor accessor, Message<?> message) {
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        
        if (sessionId == null || destination == null) {
            return message;
        }
        
        // Normalizar el topic
        String normalizedTopic = destination.startsWith("/topic/") 
            ? destination.substring(7) 
            : destination.startsWith("topic/") 
                ? destination.substring(6) 
                : destination;
        
        // Topics del sistema siempre permitidos
        if (normalizedTopic.startsWith("system") || normalizedTopic.startsWith("user/")) {
            webSocketSessionService.addSubscription(sessionId, destination);
            return message;
        }
        
        // Verificar acceso para topics de módulos específicos
        List<ModuleResponse> userModules = webSocketSessionService.getUserModules(sessionId);
        String userId = webSocketSessionService.getUserId(sessionId);
        
        if (userModules == null || userModules.isEmpty() || 
            !webSocketModuleMappingService.hasAccessToTopic(destination, userModules)) {
            log.warn("🚫 [WebSocket] Suscripción bloqueada: sesión {} (usuario {}) → {}", 
                sessionId, userId, destination);
            return null;
        }
        
        webSocketSessionService.addSubscription(sessionId, destination);
        log.info("✅ [WebSocket] Suscripción permitida: sesión {} (usuario {}) → {}", sessionId, userId, destination);
        return message;
    }
    
    private Message<?> handleDisconnect(StompHeaderAccessor accessor, Message<?> message) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            webSocketSessionService.removeSession(sessionId);
            log.info("🔌 [WebSocket] Sesión {} desconectada y limpiada", sessionId);
        }
        return message;
    }
    
    private boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private Claims getClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
