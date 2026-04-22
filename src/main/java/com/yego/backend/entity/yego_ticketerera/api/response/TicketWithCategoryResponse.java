package com.yego.backend.entity.yego_ticketerera.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de response para ticket con información de categoría en el sistema YEGO Ticketerera
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketWithCategoryResponse {
    
    private Long id;
    private String ticketNumber;
    private Long userId;
    private Long optionId;
    private Long moduleId;
    private Long sedeId;
    private Long agentId;
    private String licenseNumber;
    private String status;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;
    
    // Información de categorías
    private String categoryName;      // Nombre de la categoría (opción padre)
    private String subcategoryName;   // Nombre de la subcategoría (opción actual)
    private String categoryDescription;
    private String subcategoryDescription;
}
