package com.yego.backend.entity.yego_api_externo.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para el endpoint GoBot/PPendientes
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PPendientesRequest {
    
    @NotBlank(message = "El parámetro 'telefono' es requerido")
    private String telefono;
}

