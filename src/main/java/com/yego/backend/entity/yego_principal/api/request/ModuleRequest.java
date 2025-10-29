package com.yego.backend.entity.yego_principal.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear módulos del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String descripcion;
    
    @NotBlank(message = "La URL es obligatoria")
    private String url;
    
    private String estado;
    
    @Builder.Default
    private Boolean activo = true;
}
