package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para crear logs de auditoría del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuditLogDto {
    
    @NotBlank(message = "La acción es obligatoria")
    private String action;
    
    private String resource;
    
    private String resourceId;
    
    private String details;
    
    private String ipAddress;
    
    private String userAgent;
}
