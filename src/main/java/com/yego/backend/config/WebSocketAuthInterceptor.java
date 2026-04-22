package com.yego.backend.config;

import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo;
import com.yego.backend.repository.yego_ticketerera.DispositivoRepository;
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
    private final DispositivoRepository dispositivoRepository;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor == null) {
            return message;
        }
        
        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();
        
        // Actualizar última actividad para cualquier comando (excepto CONNECT que ya lo hace)
        if (sessionId != null && !StompCommand.CONNECT.equals(command)) {
            webSocketSessionService.updateLastActivity(sessionId);
        }
        
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
            log.warn("[WebSocket] Conexión rechazada: Token de autenticación requerido");
            throw new org.springframework.messaging.MessageDeliveryException("Token de autenticación requerido");
        }
        
                String token = authHeader.substring(7);
                
                try {
            if (!validateToken(token)) {
                log.warn("[WebSocket] Token JWT inválido");
                throw new org.springframework.messaging.MessageDeliveryException("Token JWT inválido");
            }
            
                        Claims claims = getClaimsFromToken(token);

                        Integer dispositivoIdInt = claims.get("dispositivoId", Integer.class);
                        if (dispositivoIdInt != null) {
                            Integer tokenVersionClaim = claims.get("tokenVersion", Integer.class);
                            Dispositivo dispositivo = dispositivoRepository.findById(dispositivoIdInt.longValue()).orElse(null);
                            if (dispositivo == null || Boolean.FALSE.equals(dispositivo.getActive())) {
                                log.warn("[WebSocket] Dispositivo no encontrado o inactivo: {}", dispositivoIdInt);
                                throw new org.springframework.messaging.MessageDeliveryException("Dispositivo inactivo");
                            }
                            int versionActual = dispositivo.getTokenVersion() != null ? dispositivo.getTokenVersion() : 0;
                            int versionToken = tokenVersionClaim != null ? tokenVersionClaim : 0;
                            if (versionActual != versionToken) {
                                log.warn("[WebSocket] Token revocado para dispositivo {} (token={}, actual={})",
                                        dispositivoIdInt, versionToken, versionActual);
                                throw new org.springframework.messaging.MessageDeliveryException("Token revocado");
                            }

                            String deviceId = "device-" + dispositivoIdInt;
                            String tipo = claims.get("tipo", String.class);
                            UsernamePasswordAuthenticationToken deviceAuth =
                                new UsernamePasswordAuthenticationToken(
                                    deviceId,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
                                );
                            SecurityContextHolder.getContext().setAuthentication(deviceAuth);
                            accessor.setUser(deviceAuth);

                            String sessionId = accessor.getSessionId();
                            if (sessionId != null) {
                                webSocketSessionService.markAsDevice(sessionId, deviceId);
                                log.debug("[WebSocket] Dispositivo conectado: {} (tipo={}, sede={}, módulo={})",
                                    deviceId, tipo,
                                    claims.get("sedeId", Integer.class),
                                    claims.get("moduleId", Integer.class));
                            }
                            return message;
                        }

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
                        
            // CRÍTICO: Verificar que el usuario existe ANTES de autenticar
            // Esto previene conexiones con tokens válidos pero de usuarios eliminados
            Long userIdLong = userIdInt != null ? userIdInt : (userId != null && userId.matches("\\d+") ? Long.parseLong(userId) : null);
            if (userIdLong != null) {
                try {
                    // Intentar obtener módulos - si el usuario no existe, esto lanzará RuntimeException
                    List<ModuleResponse> userModules = moduleService.obtenerModulosPorUsuario(userIdLong);
                    String sessionId = accessor.getSessionId();
                    if (sessionId != null) {
                        webSocketSessionService.saveUserModules(sessionId, userModules, userId);
                        log.debug("[WebSocket] Usuario autenticado: {} (ID: {}) con rol: {} - Módulos: {}", 
                            username, userId, role, userModules.size());
                    }
                } catch (IllegalStateException e) {
                    // Límite de conexiones alcanzado
                    log.error("[WebSocket] Límite de conexiones alcanzado para usuario {}: {}", userId, e.getMessage());
                    throw new org.springframework.messaging.MessageDeliveryException("Límite de conexiones alcanzado. Intente más tarde.");
                } catch (RuntimeException e) {
                    // Usuario no encontrado o inactivo - RECHAZAR conexión INMEDIATAMENTE
                    if (e.getMessage() != null && e.getMessage().contains("Usuario no encontrado")) {
                        log.error("[WebSocket] CONEXIÓN BLOQUEADA: Usuario {} (ID: {}) no existe en la BD. Token válido pero usuario eliminado. Rechazando conexión.", username, userId);
                        // Limpiar autenticación que ya se estableció
                        SecurityContextHolder.clearContext();
                        throw new org.springframework.messaging.MessageDeliveryException("Usuario no encontrado o inactivo. Por favor, inicie sesión nuevamente.");
                    }
                    // Otros errores - solo loggear warning pero permitir conexión
                    log.warn("[WebSocket] Error obteniendo módulos del usuario {}: {}", userId, e.getMessage());
                } catch (Exception e) {
                    // Otros errores - solo loggear warning pero permitir conexión
                    log.warn("[WebSocket] Error obteniendo módulos del usuario {}: {}", userId, e.getMessage());
                }
            }
            
        } catch (org.springframework.messaging.MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[WebSocket] Error validando token: {}", e.getMessage());
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

        // Sesiones de dispositivos (TV / TABLET / TABLET_PRINCIPAL): acceso a topics de ticketera
        if (webSocketSessionService.isDeviceSession(sessionId)) {
            if (normalizedTopic.startsWith("tickets")
                || normalizedTopic.startsWith("new-ticket")
                || normalizedTopic.startsWith("ticket-")
                || normalizedTopic.startsWith("modulos-atencion")
                || normalizedTopic.startsWith("pong")) {
                webSocketSessionService.addSubscription(sessionId, destination);
                return message;
            }
            log.debug("[WebSocket] Suscripción de dispositivo bloqueada: sesión {} → {}", sessionId, destination);
            return null;
        }

        // Obtener información del usuario
        List<ModuleResponse> userModules = webSocketSessionService.getUserModules(sessionId);
        String userId = webSocketSessionService.getUserId(sessionId);
        
        if (userModules == null || userModules.isEmpty() ||
            !webSocketModuleMappingService.hasAccessToTopic(destination, userModules)) {
            log.warn("[WebSocket] Suscripción bloqueada: sesión {} (usuario {}) → {}",
                sessionId, userId, destination);
            return null;
        }
        
        webSocketSessionService.addSubscription(sessionId, destination);
        log.debug("[WebSocket] Suscripción permitida: sesión {} (usuario {}) → {}", sessionId, userId, destination);
        return message;
    }
    
    private Message<?> handleDisconnect(StompHeaderAccessor accessor, Message<?> message) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            webSocketSessionService.removeSession(sessionId);
            log.debug("[WebSocket] Sesión {} desconectada y limpiada", sessionId);
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
