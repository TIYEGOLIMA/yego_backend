package com.yego.backend.entity.yego_ticketerera.api.response;

import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO de response para módulo de atención en el sistema YEGO Ticketerera
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuloAtencionResponse {
    
    private Long id;
    private String name;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static ModuloAtencionResponse fromModuloAtencion(ModuloAtencion module) {
        return ModuloAtencionResponse.builder()
                .id(module.getId())
                .name(module.getName())
                .description(module.getDescription())
                .isActive(module.getIsActive())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .build();
    }
}
