package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para país con sus ciudades
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaisConCiudadesResponse {
    
    private Long id;
    private String nombre;
    private String simbolo_moneda;
    private List<CiudadListResponse> ciudades;
}

