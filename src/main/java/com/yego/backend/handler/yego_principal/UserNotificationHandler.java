package com.yego.backend.handler.yego_principal;

import com.yego.backend.service.yego_principal.FilteredWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handler para notificaciones WebSocket relacionadas con usuarios y roles
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserNotificationHandler {
    
    private final FilteredWebSocketService filteredWebSocketService;
    
    /**
     * Enviar notificación de logout forzado
     */
    public void enviarLogoutForzado(Long userId, String username) {
        log.info("🚨 [UserNotificationHandler] Enviando logout forzado para usuario: {} (ID: {})", username, userId);
        
        Map<String, Object> notification = Map.of(
            "type", "FORCED_LOGOUT",
            "message", "Tu cuenta ha sido actualizada por un administrador. Debes iniciar sesión nuevamente para continuar.",
            "userId", userId,
            "username", username,
            "timestamp", LocalDateTime.now().toString()
        );
        
        // Enviar SOLO al usuario específico afectado
        filteredWebSocketService.convertAndSend("/topic/user/" + userId, notification);
        
        log.info("✅ [UserNotificationHandler] Notificación de logout forzado enviada para usuario: {}", username);
    }
    
    /**
     * Enviar notificación de bloqueo de cuenta con logout automático
     */
    public void enviarBloqueoCuenta(Long userId, String username) {
        log.info("🚨 [UserNotificationHandler] Enviando notificación de bloqueo para usuario: {} (ID: {})", username, userId);
        
        Map<String, Object> notification = Map.of(
            "type", "ACCOUNT_BLOCKED",
            "message", "Tu cuenta ha sido bloqueada por un administrador.",
            "userId", userId,
            "username", username,
            "autoLogoutDelay", 5000, // 5 segundos en milisegundos
            "timestamp", LocalDateTime.now().toString()
        );
        
        // Enviar SOLO al usuario específico afectado
        filteredWebSocketService.convertAndSend("/topic/user/" + userId, notification);
        
        log.info("✅ [UserNotificationHandler] Notificación de bloqueo enviada para usuario: {}", username);
    }
    
    /**
     * Enviar notificación de desactivación de rol con logout automático
     */
    public void enviarDesactivacionRol(Long userId, String username, String roleName) {
        log.info("🚨 [UserNotificationHandler] Enviando notificación de desactivación de rol para usuario: {} (ID: {}), Rol: {}", username, userId, roleName);
        
        Map<String, Object> notification = Map.of(
            "type", "ROLE_DEACTIVATED",
            "message", "Tu rol '" + roleName + "' ha sido desactivado temporalmente. No tienes acceso al sistema en este momento.",
            "userId", userId,
            "username", username,
            "roleName", roleName,
            "autoLogoutDelay", 3000, // 3 segundos en milisegundos
            "redirectToLogin", true,
            "timestamp", LocalDateTime.now().toString()
        );
        
        // Enviar SOLO al usuario específico afectado
        filteredWebSocketService.convertAndSend("/topic/user/" + userId, notification);
        
        log.info("✅ [UserNotificationHandler] Notificación de desactivación de rol enviada para usuario: {}", username);
    }
    
    /**
     * Enviar notificación de actualización de usuarios para refrescar tabla
     */
    public void enviarActualizacionUsuarios(String action, Long userId, String username) {
        log.info("🔄 [UserNotificationHandler] Enviando notificación de actualización de usuarios: {} - Usuario: {} (ID: {})", action, username, userId);
        
        Map<String, Object> notification = Map.of(
            "type", "USER_TABLE_UPDATE",
            "action", action,
            "userId", userId,
            "username", username,
            "message", getActionMessage(action, username),
            "timestamp", LocalDateTime.now().toString()
        );
        
        // Enviar al topic del sistema para que todas las sesiones refresquen
        filteredWebSocketService.convertAndSend("/topic/system", notification);
        
        log.info("✅ [UserNotificationHandler] Notificación de actualización de usuarios enviada: {}", action);
    }
    
    /**
     * Obtener mensaje descriptivo para la acción
     */
    private String getActionMessage(String action, String username) {
        switch (action) {
            case "USER_CREATED":
                return "Se ha creado un nuevo usuario: " + username;
            case "USER_UPDATED":
                return "Se ha actualizado el usuario: " + username;
            case "USER_DELETED":
                return "Se ha eliminado el usuario: " + username;
            case "USER_STATUS_CHANGED":
                return "Se ha cambiado el estado del usuario: " + username;
            case "USER_PASSWORD_CHANGED":
                return "Se ha cambiado la contraseña del usuario: " + username;
            default:
                return "Se ha modificado el usuario: " + username;
        }
    }
}

