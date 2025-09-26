package com.yego.backend.entity.yego_ticketerera.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de response para el estado del módulo de un usuario en el sistema YEGO Ticketerera
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserModuleStatusResponse {
    
    private Long userId;
    private Long moduleId;
    private String status;           // OCUPADO, LIBRE, DISPONIBLE
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
