package com.yego.backend.config;

import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo;
import com.yego.backend.repository.yego_ticketerera.DispositivoRepository;
import com.yego.backend.service.yego_principal.ModuleService;
import com.yego.backend.service.yego_principal.WebSocketModuleMappingService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.Locale;

/**
 * Interceptor de autenticación para WebSocket
 * Valida tokens JWT en las conexiones WebSocket
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    private final ModuleService moduleService;
    private final WebSocketSessionService webSocketSessionService;
    private final WebSocketModuleMappingService webSocketModuleMappingService;
    private final DispositivoRepository dispositivoRepository;
    private final JwtTokenProvider jwtTokenProvider;
    
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
                        Claims claims = getClaimsFromToken(token);

                        Long dispositivoId = getLongClaim(claims, "dispositivoId");
                        if (dispositivoId != null) {
                            Integer tokenVersionClaim = getIntegerClaim(claims, "tokenVersion");
                            Dispositivo dispositivo = dispositivoRepository.findById(dispositivoId).orElse(null);
                            if (dispositivo == null || Boolean.FALSE.equals(dispositivo.getActive())) {
                                log.debug("[WebSocket] Dispositivo no encontrado o inactivo: {}", dispositivoId);
                                throw new org.springframework.messaging.MessageDeliveryException("Dispositivo inactivo");
                            }
                            int versionActual = dispositivo.getTokenVersion() != null ? dispositivo.getTokenVersion() : 0;
                            int versionToken = tokenVersionClaim != null ? tokenVersionClaim : 0;
                            if (versionActual != versionToken) {
                                log.warn("[WebSocket] Token revocado para dispositivo {} (token={}, actual={})",
                                        dispositivoId, versionToken, versionActual);
                                throw new org.springframework.messaging.MessageDeliveryException("Token revocado");
                            }

                            String deviceId = "device-" + dispositivoId;
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
                                    getLongClaim(claims, "sedeId"),
                                    getLongClaim(claims, "moduleId"));
                            }
                            return message;
                        }

                        String username = claims.get("username", String.class);
                        if (username == null) {
                            username = claims.getSubject();
                        }
                        String role = claims.get("role", String.class);
                        if (username == null || username.isBlank() || role == null || role.isBlank()) {
                            throw new org.springframework.messaging.MessageDeliveryException("Token JWT inválido");
                        }

                        Long userIdLong = getLongClaim(claims, "userId");
                        boolean mobileDriver = "CONDUCTOR".equalsIgnoreCase(role);
                        if (userIdLong == null && !mobileDriver) {
                            throw new org.springframework.messaging.MessageDeliveryException("Token JWT inválido");
                        }

                        String userId = userIdLong != null ? userIdLong.toString() : username;
                        List<ModuleResponse> userModules = List.of();
                        if (userIdLong != null) {
                            try {
                                userModules = moduleService.obtenerModulosPorUsuario(userIdLong);
                            } catch (IllegalStateException exception) {
                                log.warn("[WebSocket] Conexión rechazada para usuario {}: límite alcanzado", userId);
                                throw new org.springframework.messaging.MessageDeliveryException(
                                        "Límite de conexiones alcanzado. Intente más tarde.");
                            } catch (RuntimeException exception) {
                                log.debug("[WebSocket] Usuario rechazado: id={} ({})",
                                        userId, exception.getClass().getSimpleName());
                                throw new org.springframework.messaging.MessageDeliveryException(
                                        "Usuario no encontrado o inactivo");
                            }
                        }

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userId,
                                        null,
                                        List.of(new SimpleGrantedAuthority(
                                                "ROLE_" + role.toUpperCase(Locale.ROOT))));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);

                        String sessionId = accessor.getSessionId();
                        if (sessionId != null && userIdLong != null) {
                            webSocketSessionService.saveUserModules(sessionId, userModules, userId);
                            log.debug("[WebSocket] Usuario autenticado: id={} rol={} módulos={}",
                                    userId, role, userModules.size());
                        }
            
        } catch (org.springframework.messaging.MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.debug("[WebSocket] Token rechazado ({})", e.getClass().getSimpleName());
            throw new org.springframework.messaging.MessageDeliveryException("Token JWT inválido");
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
    
    private Claims getClaimsFromToken(String token) {
        return jwtTokenProvider.parse(token);
    }

    private static Long getLongClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Integer getIntegerClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value instanceof Number number ? number.intValue() : null;
    }
}
