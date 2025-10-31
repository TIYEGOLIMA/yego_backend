package com.yego.backend.entity.yego_garantizado.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para país con sus ciudades y cálculos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaisCalculoRequest {
    
    private String pais;
    private List<CiudadCalculoRequest> ciudades;
}

