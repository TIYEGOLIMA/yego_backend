package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para permisos del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponseDto {
    
    private Long id;
    private String name;
    private String description;
    private String module;
    private String action;
    private String conditions;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
