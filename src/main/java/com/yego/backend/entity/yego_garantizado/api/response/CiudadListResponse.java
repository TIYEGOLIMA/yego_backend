package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta simplificada para listado de ciudades
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiudadListResponse {
    
    private Long id;
    private String nombre;
    private Long pais_id;
}

