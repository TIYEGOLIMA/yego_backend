package com.yego.backend.config;

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
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("🔌 [WebSocket] Intentando conectar...");
            
            // Obtener token del header Authorization
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            log.info("🔍 [WebSocket] Auth header: {}", authHeader != null ? "Presente" : "Ausente");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                try {
                    // Validar token JWT
                    if (validateToken(token)) {
                        Claims claims = getClaimsFromToken(token);
                        String username = claims.getSubject();
                        String role = claims.get("role", String.class);
                        Integer userIdInt = claims.get("userId", Integer.class);
                        String userId = userIdInt != null ? userIdInt.toString() : username;
                        
                        // Crear autenticación
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                userId, 
                                null, 
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );
                        
                        // Establecer en el contexto de seguridad
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);
                        
                        log.info("🔐 [WebSocket] Usuario autenticado: {} con rol: {}", username, role);
                        
                    } else {
                        log.warn("⚠️ [WebSocket] Token JWT inválido");
                        throw new RuntimeException("Token JWT inválido");
                    }
                    
                } catch (Exception e) {
                    log.error("❌ [WebSocket] Error validando token: {}", e.getMessage());
                    throw new RuntimeException("Error de autenticación WebSocket");
                }
            } else {
                log.warn("⚠️ [WebSocket] No se encontró token de autorización");
                throw new RuntimeException("Token de autorización requerido");
            }
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
