package com.yego.backend.entity.yego_ticketerera.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de request para crear una calificación en el sistema YEGO Ticketerera
 */
@Data
public class CrearRatingRequest {
    
    @NotNull(message = "El ticket_id es obligatorio")
    private Long ticketId;
    
    @NotNull(message = "El score es obligatorio")
    @Min(value = 1, message = "El score debe ser mínimo 1")
    @Max(value = 5, message = "El score debe ser máximo 5")
    private Integer score;
    
    private String comment;
    
    private LocalDateTime timestamp; // Timestamp del frontend
}
