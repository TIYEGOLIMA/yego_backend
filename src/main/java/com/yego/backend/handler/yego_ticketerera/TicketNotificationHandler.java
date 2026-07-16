package com.yego.backend.handler.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.entity.yego_ticketerera.api.response.ModulosEstadoResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWithCategoryResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWebSocketResponse;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yego.backend.service.yego_principal.FilteredWebSocketService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.time.Instant;

/**
 * Handler para notificaciones WebSocket relacionadas con tickets
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketNotificationHandler {
    
    private final FilteredWebSocketService filteredWebSocketService;
    private final OptionRepository optionRepository;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Enviar nuevo ticket creado
     */
    public void enviarNuevoTicket(Ticket ticket) {
        log.info("[TicketNotificationHandler] Enviando nuevo ticket por WebSocket: {}", ticket.getTicketNumber());
        
        // Obtener información completa del ticket
        TicketWebSocketResponse ticketCompleto = obtenerInformacionCompletaTicket(ticket);
        
        filteredWebSocketService.convertAndSend("/topic/tickets", ticketCompleto);
        filteredWebSocketService.convertAndSend("/topic/new-ticket", ticketCompleto);
        publicarEventoTicket("TICKET_CREATED", ticket.getSedeId(), ticket.getModuleId(), ticketCompleto);
    }
    
    /**
     * Enviar ticket llamado
     */
    public void enviarTicketLlamado(TicketWithCategoryResponse ticketCompleto) {
        log.info("[TicketNotificationHandler] Enviando ticket llamado por WebSocket: {}", ticketCompleto.getTicketNumber());
        filteredWebSocketService.convertAndSend("/topic/tickets", ticketCompleto);
        filteredWebSocketService.convertAndSend("/topic/ticket-called", ticketCompleto);
        publicarEventoTicket("TICKET_CALLED", ticketCompleto.getSedeId(), ticketCompleto.getModuleId(), ticketCompleto);
    }
    
    /**
     * Enviar ticket iniciado
     */
    public void enviarTicketIniciado(TicketWithCategoryResponse ticketCompleto) {
        log.info("[TicketNotificationHandler] Enviando ticket iniciado por WebSocket: {}", ticketCompleto.getTicketNumber());
        filteredWebSocketService.convertAndSend("/topic/tickets", ticketCompleto);
        filteredWebSocketService.convertAndSend("/topic/ticket-started", ticketCompleto);
        publicarEventoTicket("TICKET_STARTED", ticketCompleto.getSedeId(), ticketCompleto.getModuleId(), ticketCompleto);
    }
    
    /**
     * Enviar ticket completado
     */
    public void enviarTicketCompletado(TicketWithCategoryResponse ticketCompleto) {
        log.info("[TicketNotificationHandler] Enviando ticket completado por WebSocket: {}", ticketCompleto.getTicketNumber());
        filteredWebSocketService.convertAndSend("/topic/tickets", ticketCompleto);
        filteredWebSocketService.convertAndSend("/topic/ticket-completed", ticketCompleto);
        publicarEventoTicket("TICKET_COMPLETED", ticketCompleto.getSedeId(), ticketCompleto.getModuleId(), ticketCompleto);
        if (ticketCompleto.getSedeId() != null) {
            String sedeTopic = "/topic/ticketera/sede/" + ticketCompleto.getSedeId() + "/rating";
            filteredWebSocketService.convertAndSend(sedeTopic, ticketCompleto);
            log.info("[TicketNotificationHandler] Enviando rating a tablet sede {}: {}", ticketCompleto.getSedeId(), sedeTopic);
        }
        if (ticketCompleto.getSedeId() != null && ticketCompleto.getModuleId() != null) {
            String ratingTopic = "/topic/ticketera/sedes/" + ticketCompleto.getSedeId()
                    + "/modules/" + ticketCompleto.getModuleId() + "/rating";
            filteredWebSocketService.convertAndSend(
                    ratingTopic,
                    crearEvento("TICKET_COMPLETED", ticketCompleto.getSedeId(),
                            ticketCompleto.getModuleId(), ticketCompleto));
        }
    }
    
    /**
     * Enviar ticket cancelado
     */
    public void enviarTicketCancelado(Ticket ticket) {
        log.info("[TicketNotificationHandler] Enviando ticket cancelado por WebSocket: {}", ticket.getTicketNumber());
        filteredWebSocketService.convertAndSend("/topic/tickets", ticket);
        filteredWebSocketService.convertAndSend("/topic/ticket-cancelled", ticket);
        publicarEventoTicket("TICKET_CANCELLED", ticket.getSedeId(), ticket.getModuleId(), ticket);
    }
    
    /**
     * Enviar lista actualizada de módulos de atención (disponibles y ocupados)
     */
    public void enviarModulosActualizados(ModulosEstadoResponse modulosEstado) {
        try {
            java.util.Map<String, Object> notification = new java.util.HashMap<>();
            notification.put("type", "MODULOS_ACTUALIZADOS");
            notification.put("modulosDisponibles", modulosEstado.getModulosDisponibles());
            notification.put("modulosOcupados", modulosEstado.getModulosOcupados());
            notification.put("timestamp", java.time.LocalDateTime.now().toString());
            
            log.info("[TicketNotificationHandler] Enviando módulos actualizados - Disponibles: {}, Ocupados: {}", 
                modulosEstado.getModulosDisponibles().size(), 
                modulosEstado.getModulosOcupados().size());
            log.debug("[TicketNotificationHandler] Contenido del mensaje: type={}, modulosDisponibles={}, modulosOcupados={}", 
                notification.get("type"), 
                modulosEstado.getModulosDisponibles().size(), 
                modulosEstado.getModulosOcupados().size());
            
            // Enviar a /topic/modulos-atencion - esto afecta a TODOS los usuarios en sesión con acceso a "tickets"
            filteredWebSocketService.convertAndSend("/topic/modulos-atencion", notification);
            if (modulosEstado.getSedeId() != null) {
                filteredWebSocketService.convertAndSend(
                        "/topic/ticketera/sedes/" + modulosEstado.getSedeId() + "/modules",
                        crearEvento("MODULES_UPDATED", modulosEstado.getSedeId(), null, modulosEstado));
            }
            
            log.info("[TicketNotificationHandler] Notificación WebSocket enviada correctamente a /topic/modulos-atencion");
        } catch (Exception e) {
            log.error("[TicketNotificationHandler] Error enviando módulos actualizados por WebSocket: {}", e.getMessage(), e);
        }
    }

    private void publicarEventoTicket(String type, Long sedeId, Long moduleId, Object data) {
        if (sedeId == null) return;
        filteredWebSocketService.convertAndSend(
                "/topic/ticketera/sedes/" + sedeId + "/tickets",
                crearEvento(type, sedeId, moduleId, data));
    }

    private Map<String, Object> crearEvento(String type, Long sedeId, Long moduleId, Object data) {
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("type", type);
        event.put("occurredAt", Instant.now().toString());
        event.put("sedeId", sedeId);
        event.put("moduleId", moduleId);
        event.put("data", data);
        return event;
    }
    
    /**
     * Obtener información completa del ticket
     */
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
            String driverName = obtenerNombreConductor(ticket.getLicenseNumber());
            
            // Construir respuesta completa
            TicketWebSocketResponse response = TicketWebSocketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .priority(ticket.getPriority())
                .userId(ticket.getUserId())
                .moduleId(ticket.getModuleId())
                .sedeId(ticket.getSedeId())
                .licenseNumber(ticket.getLicenseNumber())
                .optionId(ticket.getOptionId())
                .categoryName(categoryName)
                .categoryDescription(categoryDescription)
                .subcategoryName(subcategoryName)
                .subcategoryDescription(subcategoryDescription)
                .driverName(driverName)
                .build();
            
            log.info("[TicketNotificationHandler] Información completa del ticket {} obtenida", ticket.getTicketNumber());
            return response;
            
        } catch (Exception e) {
            log.error("[TicketNotificationHandler] Error obteniendo información completa del ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
            
            // Fallback: respuesta básica sin información adicional
            return TicketWebSocketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .priority(ticket.getPriority())
                .userId(ticket.getUserId())
                .moduleId(ticket.getModuleId())
                .sedeId(ticket.getSedeId())
                .licenseNumber(ticket.getLicenseNumber())
                .optionId(ticket.getOptionId())
                .categoryName("Información del servicio")
                .categoryDescription("")
                .subcategoryName("")
                .subcategoryDescription("")
                .driverName(obtenerNombreConductor(ticket.getLicenseNumber()))
                .build();
        }
    }
    
    /**
     * Obtiene el nombre del conductor por su número de licencia (phone)
     */
    private String obtenerNombreConductor(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.isEmpty()) {
            return "";
        }
        
        try {
            String sql = "SELECT full_name FROM drivers WHERE phone = ?";
            String fullName = jdbcTemplate.queryForObject(sql, String.class, licenseNumber);
            return (fullName != null && !fullName.isEmpty()) ? fullName : "";
        } catch (Exception e) {
            log.warn("[TicketNotificationHandler] No se pudo obtener nombre del conductor para licenseNumber: {}", licenseNumber);
            return "";
        }
    }
}
