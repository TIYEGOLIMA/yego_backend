package com.yego.backend.entity.yego_garantizado.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear un país
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearPaisRequest {
    
    private String nombre;
    private String moneda;
    private String simbolo_moneda;
}

