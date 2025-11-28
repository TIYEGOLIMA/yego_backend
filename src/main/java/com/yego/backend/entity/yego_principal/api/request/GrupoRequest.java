package com.yego.backend.entity.yego_principal.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar grupos del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String icono;
    
    @Builder.Default
    private Boolean activo = true;
}

