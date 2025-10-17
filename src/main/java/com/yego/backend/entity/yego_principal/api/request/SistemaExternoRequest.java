package com.yego.backend.entity.yego_principal.api.request;

import com.yego.backend.entity.yego_principal.entities.SistemaExterno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para request de sistema externo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SistemaExternoRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String nombre;
    
    @Size(max = 255, message = "La descripción no puede tener más de 255 caracteres")
    private String descripcion;
    
    @Pattern(regexp = "^https?://.*$", message = "La URL debe comenzar con http:// o https://")
    private String url;
    
    private SistemaExterno.EstadoSistema estado;
    
    
    private Boolean activo;
}
