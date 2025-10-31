package com.yego.backend.entity.yego_garantizado.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una ciudad
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearCiudadRequest {
    
    private Long pais_id;
    private String nombre;
}

