package com.yego.backend.service;

import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWithCategoryResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWebSocketResponse;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    
}
