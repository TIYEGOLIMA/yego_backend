package com.yego.backend.entity.yego_ticketerera.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de response para recuperar información de módulo en el sistema YEGO Ticketerera
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecuperarModuloResponse {
    
    private Long moduleId;
    private String status;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
