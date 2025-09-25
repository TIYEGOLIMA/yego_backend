package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * DTO para crear permisos del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionDto {
    
    @NotBlank(message = "El nombre del permiso es obligatorio")
    private String name;
    
    private String description;
    
    @NotBlank(message = "El módulo es obligatorio")
    private String module;
    
    @NotBlank(message = "La acción es obligatoria")
    private String action;
    
    private Map<String, Object> conditions;
    
    @Builder.Default
    private Boolean active = true;
}
