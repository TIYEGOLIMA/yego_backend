package com.yego.backend.config;

import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo;
import com.yego.backend.repository.yego_ticketerera.DispositivoRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_principal.ModuleService;
import com.yego.backend.service.yego_principal.WebSocketAccessTicketService;
import com.yego.backend.service.yego_principal.WebSocketModuleMappingService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import com.yego.backend.service.yego_principal.WebSocketSessionService.SessionContext;
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
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebSocketAccessTicketService accessTicketService;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor == null) {
            return message;
        }
        
        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();

        if (sessionId != null && !StompCommand.CONNECT.equals(command)
                && webSocketSessionService.isSessionExpired(sessionId)) {
            webSocketSessionService.removeSession(sessionId);
            throw new org.springframework.messaging.MessageDeliveryException("Sesión WebSocket expirada");
        }
        
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

        // Ticketera es command/query: las mutaciones viajan por HTTP y STOMP solo notifica.
        if (StompCommand.SEND.equals(command)
                && accessor.getDestination() != null
                && accessor.getDestination().startsWith("/app/ticketera")) {
            throw new org.springframework.messaging.MessageDeliveryException(
                    "Las acciones de Ticketera deben realizarse por HTTP");
        }
        
        // Manejar desconexión (DISCONNECT)
        if (StompCommand.DISCONNECT.equals(command)) {
            return handleDisconnect(accessor, message);
        }
        
        return message;
    }
    
    private Message<?> handleConnect(StompHeaderAccessor accessor, Message<?> message) {
            String accessTicket = accessor.getFirstNativeHeader("X-WS-Ticket");
            if (accessTicket != null && !accessTicket.isBlank()) {
                authenticateWithAccessTicket(accessor, accessTicket);
                return message;
            }

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
                                webSocketSessionService.saveSessionContext(sessionId, new SessionContext(
                                        true,
                                        "DEVICE",
                                        tipo,
                                        getLongClaim(claims, "sedeId"),
                                        getLongClaim(claims, "moduleId"),
                                        claims.getExpiration() != null ? claims.getExpiration().toInstant() : null));
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
                            Long sedeId = userRepository.findById(userIdLong).map(user -> user.getSedeId()).orElse(null);
                            webSocketSessionService.saveSessionContext(sessionId, new SessionContext(
                                    false,
                                    role.toUpperCase(Locale.ROOT),
                                    null,
                                    sedeId,
                                    null,
                                    claims.getExpiration() != null ? claims.getExpiration().toInstant() : null));
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

    private void authenticateWithAccessTicket(StompHeaderAccessor accessor, String ticket) {
        var principal = accessTicketService.consume(ticket);
        String sessionId = accessor.getSessionId();

        if (principal.isDevice()) {
            Dispositivo dispositivo = dispositivoRepository.findById(principal.dispositivoId()).orElse(null);
            if (dispositivo == null || Boolean.FALSE.equals(dispositivo.getActive())) {
                throw new org.springframework.messaging.MessageDeliveryException("Dispositivo inactivo");
            }
            int currentVersion = dispositivo.getTokenVersion() != null ? dispositivo.getTokenVersion() : 0;
            int ticketVersion = principal.tokenVersion() != null ? principal.tokenVersion() : 0;
            if (currentVersion != ticketVersion) {
                throw new org.springframework.messaging.MessageDeliveryException("Token de dispositivo revocado");
            }

            String deviceId = "device-" + dispositivo.getId();
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    deviceId, null, List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            accessor.setUser(authentication);
            if (sessionId != null) {
                webSocketSessionService.markAsDevice(sessionId, deviceId);
                webSocketSessionService.saveSessionContext(sessionId, new SessionContext(
                        true,
                        "DEVICE",
                        dispositivo.getType().name(),
                        dispositivo.getSedeId(),
                        dispositivo.getModuleId(),
                        principal.tokenExpiresAt()));
            }
            return;
        }

        if (principal.userId() == null || principal.username() == null || principal.role() == null) {
            throw new org.springframework.messaging.MessageDeliveryException("Usuario WebSocket inválido");
        }
        var user = userRepository.findByIdWithRole(principal.userId())
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new org.springframework.messaging.MessageDeliveryException("Usuario inactivo"));
        String role = user.getRole().getName().toUpperCase(Locale.ROOT);
        List<ModuleResponse> modules = moduleService.obtenerModulosPorUsuario(user.getId());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getId().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        accessor.setUser(authentication);
        if (sessionId != null) {
            webSocketSessionService.saveUserModules(sessionId, modules, user.getId().toString());
            webSocketSessionService.saveSessionContext(sessionId, new SessionContext(
                    false, role, null, user.getSedeId(), null, principal.tokenExpiresAt()));
        }
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

        if (normalizedTopic.startsWith("ticketera/sedes/")) {
            if (isScopedTicketeraSubscriptionAllowed(sessionId, normalizedTopic)) {
                webSocketSessionService.addSubscription(sessionId, destination);
                return message;
            }
            log.debug("[WebSocket] Suscripción Ticketera fuera de alcance: sesión {} → {}", sessionId, destination);
            return null;
        }
        
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

    private boolean isScopedTicketeraSubscriptionAllowed(String sessionId, String topic) {
        SessionContext context = webSocketSessionService.getSessionContext(sessionId);
        if (context == null) return false;

        String[] parts = topic.split("/");
        if (parts.length < 4 || !"ticketera".equals(parts[0]) || !"sedes".equals(parts[1])) return false;
        Long topicSedeId = parseLong(parts[2]);
        if (topicSedeId == null) return false;

        boolean ticketsTopic = parts.length == 4 && "tickets".equals(parts[3]);
        boolean modulesTopic = parts.length == 4 && "modules".equals(parts[3]);
        boolean ratingTopic = parts.length == 6
                && "modules".equals(parts[3])
                && "rating".equals(parts[5]);
        if (!ticketsTopic && !modulesTopic && !ratingTopic) return false;

        if (context.device()) {
            if (!topicSedeId.equals(context.sedeId())) return false;
            if ("TABLET".equals(context.deviceType())) {
                Long topicModuleId = ratingTopic ? parseLong(parts[4]) : null;
                return topicModuleId != null && topicModuleId.equals(context.moduleId());
            }
            return !ratingTopic && ("TV".equals(context.deviceType())
                    || "TABLET_PRINCIPAL".equals(context.deviceType()));
        }

        String role = context.role() != null ? context.role().toUpperCase(Locale.ROOT) : "";
        if (ratingTopic) return false;
        if ("SAC".equals(role)) return topicSedeId.equals(context.sedeId());
        return List.of("ADMIN", "ADMINISTRADOR", "SUPERVISOR", "SUPERADMIN").contains(role)
                && webSocketModuleMappingService.hasAccessToTopic("/topic/ticketera",
                        webSocketSessionService.getUserModules(sessionId));
    }

    private static Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
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
