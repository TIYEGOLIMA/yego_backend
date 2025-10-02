package com.yego.backend.entity.yego_ticketerera.api.response;

import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuesta WebSocket de tickets
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketWebSocketResponse {
    
    private Long id;
    private String ticketNumber;
    private Ticket.TicketStatus status;
    private LocalDateTime createdAt;
    private Integer priority; // Cambiado de Ticket.Priority a Integer
    private Long userId;
    private Long moduleId;
    private String licenseNumber;
    private Long optionId;
    private String categoryName;
    private String categoryDescription;
    private String subcategoryName;
    private String subcategoryDescription;
    private String driverName;
}
