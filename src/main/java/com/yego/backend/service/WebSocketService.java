package com.yego.backend.service;

import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWithCategoryResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWebSocketResponse;
import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoResponse;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Servicio WebSocket centralizado - Basado en ticketera_backend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final OptionRepository optionRepository;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Enviar nuevo ticket creado
     */
    public void enviarNuevoTicket(Ticket ticket) {
        log.info("📤 Enviando nuevo ticket por WebSocket: {}", ticket.getTicketNumber());
        
        // Obtener información completa del ticket
        TicketWebSocketResponse ticketCompleto = obtenerInformacionCompletaTicket(ticket);
        
        messagingTemplate.convertAndSend("/topic/tickets", ticketCompleto);
        messagingTemplate.convertAndSend("/topic/new-ticket", ticketCompleto);
    }
    
    /**
     * Enviar ticket llamado
     */
    public void enviarTicketLlamado(TicketWithCategoryResponse ticketCompleto) {
        log.info("📤 Enviando ticket llamado por WebSocket: {}", ticketCompleto.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/tickets", ticketCompleto);
        messagingTemplate.convertAndSend("/topic/ticket-called", ticketCompleto);
    }
    
    /**
     * Enviar ticket iniciado
     */
    public void enviarTicketIniciado(TicketWithCategoryResponse ticketCompleto) {
        log.info("📤 Enviando ticket iniciado por WebSocket: {}", ticketCompleto.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/tickets", ticketCompleto);
        messagingTemplate.convertAndSend("/topic/ticket-started", ticketCompleto);
    }
    
    /**
     * Enviar ticket completado
     */
    public void enviarTicketCompletado(TicketWithCategoryResponse ticketCompleto) {
        log.info("📤 Enviando ticket completado por WebSocket: {}", ticketCompleto.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/tickets", ticketCompleto);
        messagingTemplate.convertAndSend("/topic/ticket-completed", ticketCompleto);
    }
    
    /**
     * Enviar ticket cancelado
     */
    public void enviarTicketCancelado(Ticket ticket) {
        log.info("📤 Enviando ticket cancelado por WebSocket: {}", ticket.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/tickets", ticket);
        messagingTemplate.convertAndSend("/topic/ticket-cancelled", ticket);
    }
    
    /**
     * LEGACY: Mantener compatibilidad con método anterior
     */
    public void sendTicketeraEvent(String event, Object data) {
        Map<String, Object> notification = Map.of(
            "type", event,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/ticketera", notification);
        log.info("📤 [WebSocket] Ticketera: {} - Enviado a /topic/ticketera", event);
    }
    
    /**
     * Enviar notificación a OKR
     */
    public void sendOkrEvent(String event, Object data) {
        Map<String, Object> notification = Map.of(
            "event", event,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/okr", notification);
        log.info("📤 [WebSocket] OKR: {}", event);
    }
    
    /**
     * Enviar notificación a Marketing
     */
    public void sendMarketingEvent(String event, Object data) {
        Map<String, Object> notification = Map.of(
            "event", event,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/marketing", notification);
        log.info("📤 [WebSocket] Marketing: {}", event);
    }
    
    /**
     * Enviar notificación global del sistema
     */
    public void sendSystemEvent(String event, Object data) {
        Map<String, Object> notification = Map.of(
            "event", event,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/system", notification);
        log.info("📤 [WebSocket] Sistema: {}", event);
    }
    
    // 🚀 MÉTODO: Obtener información completa del ticket
    private TicketWebSocketResponse obtenerInformacionCompletaTicket(Ticket ticket) {
        try {
            // Obtener información de la opción seleccionada
            String categoryName = "Información del servicio";
            String categoryDescription = "";
            String subcategoryName = "";
            String subcategoryDescription = "";
            
            if (ticket.getOptionId() != null) {
                var option = optionRepository.findById(ticket.getOptionId()).orElse(null);
                if (option != null) {
                    if (option.getParentId() != null) {
                        // Es una subcategoría, obtener la categoría padre
                        var parentOption = optionRepository.findById(option.getParentId()).orElse(null);
                        if (parentOption != null) {
                            categoryName = parentOption.getName();
                            categoryDescription = parentOption.getDescription() != null ? parentOption.getDescription() : "";
                            subcategoryName = option.getName();
                            subcategoryDescription = option.getDescription() != null ? option.getDescription() : "";
                        } else {
                            categoryName = option.getName();
                            categoryDescription = option.getDescription() != null ? option.getDescription() : "";
                            subcategoryName = "";
                            subcategoryDescription = "";
                        }
                    } else {
                        // Es una categoría principal
                        categoryName = option.getName();
                        categoryDescription = option.getDescription() != null ? option.getDescription() : "";
                        subcategoryName = "";
                        subcategoryDescription = "";
                    }
                }
            }
            
            // Obtener nombre del conductor por licenseNumber
            String driverName = "";
            if (ticket.getLicenseNumber() != null && !ticket.getLicenseNumber().isEmpty()) {
                try {
                    String sql = "SELECT full_name FROM drivers WHERE phone = ?";
                    String fullName = jdbcTemplate.queryForObject(sql, String.class, ticket.getLicenseNumber());
                    if (fullName != null && !fullName.isEmpty()) {
                        driverName = fullName;
                    }
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo obtener nombre del conductor para licenseNumber: {}", ticket.getLicenseNumber());
                }
            }
            
            // Construir respuesta completa
            TicketWebSocketResponse response = TicketWebSocketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .priority(ticket.getPriority())
                .userId(ticket.getUserId())
                .moduleId(ticket.getModuleId())
                .licenseNumber(ticket.getLicenseNumber())
                .optionId(ticket.getOptionId())
                .categoryName(categoryName)
                .categoryDescription(categoryDescription)
                .subcategoryName(subcategoryName)
                .subcategoryDescription(subcategoryDescription)
                .driverName(driverName)
                .build();
            
            log.info("✅ Información completa del ticket {} obtenida", ticket.getTicketNumber());
            return response;
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo información completa del ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
            
            // Obtener nombre del conductor en fallback también
            String driverName = "";
            if (ticket.getLicenseNumber() != null && !ticket.getLicenseNumber().isEmpty()) {
                try {
                    String sql = "SELECT full_name FROM drivers WHERE phone = ?";
                    String fullName = jdbcTemplate.queryForObject(sql, String.class, ticket.getLicenseNumber());
                    if (fullName != null && !fullName.isEmpty()) {
                        driverName = fullName;
                    }
                } catch (Exception ex) {
                    log.warn("⚠️ No se pudo obtener nombre del conductor para licenseNumber: {}", ticket.getLicenseNumber());
                }
            }
            
            // Fallback: respuesta básica
            return TicketWebSocketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .priority(ticket.getPriority())
                .userId(ticket.getUserId())
                .moduleId(ticket.getModuleId())
                .licenseNumber(ticket.getLicenseNumber())
                .optionId(ticket.getOptionId())
                .categoryName("Información del servicio")
                .categoryDescription("")
                .subcategoryName("")
                .subcategoryDescription("")
                .driverName(driverName)
                .build();
        }
    }
    
    /**
     * Enviar notificación de logout forzado
     */
    public void enviarLogoutForzado(Long userId, String username) {
        log.info("🚨 Enviando logout forzado para usuario: {} (ID: {})", username, userId);
        
        Map<String, Object> notification = Map.of(
            "type", "FORCED_LOGOUT",
            "message", "Tu cuenta ha sido actualizada por un administrador. Debes iniciar sesión nuevamente para continuar.",
            "userId", userId,
            "username", username,
            "timestamp", LocalDateTime.now().toString()
        );
        
        // Enviar al topic del sistema para que todos los usuarios conectados lo reciban
        messagingTemplate.convertAndSend("/topic/system", notification);
        
        // También enviar a un topic específico del usuario (si está implementado)
        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
        
        log.info("✅ Notificación de logout forzado enviada para usuario: {}", username);
    }
    
    /**
     * Enviar notificación de bloqueo de cuenta con logout automático
     */
    public void enviarBloqueoCuenta(Long userId, String username) {
        log.info("🚨 Enviando notificación de bloqueo para usuario: {} (ID: {})", username, userId);
        
        Map<String, Object> notification = Map.of(
            "type", "ACCOUNT_BLOCKED",
            "message", "Tu cuenta ha sido bloqueada por un administrador.",
            "userId", userId,
            "username", username,
            "autoLogoutDelay", 5000, // 5 segundos en milisegundos
            "timestamp", LocalDateTime.now().toString()
        );
        
        // Enviar al topic del sistema
        messagingTemplate.convertAndSend("/topic/system", notification);
        
        // También enviar a un topic específico del usuario
        messagingTemplate.convertAndSend("/topic/user/" + userId, notification);
        
        log.info("✅ Notificación de bloqueo enviada para usuario: {}", username);
    }
    

    /**
     * Enviar datos completos de garantizado para actualizar la tabla
     */
    public void enviarDatosCompletosGarantizado(List<GarantizadoResponse> conductores, String semanaActual) {
        log.info("📊 Enviando datos completos de garantizado - {} conductores para semana {}", conductores.size(), semanaActual);

        Map<String, Object> data = Map.of(
            "type", "GARANTIZADO_TABLE_UPDATE",
            "semanaActual", semanaActual,
            "conductores", conductores,
            "totalConductores", conductores.size(),
            "timestamp", LocalDateTime.now().toString()
        );

        // Enviar al topic del sistema
        messagingTemplate.convertAndSend("/topic/system", data);

        // Enviar a topic específico de garantizado
        messagingTemplate.convertAndSend("/topic/garantizado", data);

        log.info("✅ Datos completos de garantizado enviados - {} conductores", conductores.size());
    }

    /**
     * Enviar notificación de actualización de usuarios para refrescar tabla
     */
    public void enviarActualizacionUsuarios(String action, Long userId, String username) {
        log.info("🔄 Enviando notificación de actualización de usuarios: {} - Usuario: {} (ID: {})", action, username, userId);
        
        Map<String, Object> notification = Map.of(
            "type", "USER_TABLE_UPDATE",
            "action", action,
            "userId", userId,
            "username", username,
            "message", getActionMessage(action, username),
            "timestamp", LocalDateTime.now().toString()
        );
        
        // Enviar al topic del sistema para que todas las sesiones refresquen
        messagingTemplate.convertAndSend("/topic/system", notification);
        
        log.info("✅ Notificación de actualización de usuarios enviada: {}", action);
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
