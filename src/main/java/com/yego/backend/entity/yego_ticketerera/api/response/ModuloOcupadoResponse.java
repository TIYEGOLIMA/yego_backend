package com.yego.backend.entity.yego_ticketerera.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de response para módulo ocupado en el sistema YEGO Ticketerera
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuloOcupadoResponse {
    
    private Long moduleId;
    private Long userId;
    private String userName;
    private String status;
    private LocalDateTime horaAsignacion;
    private LocalDateTime updatedAt;
}

