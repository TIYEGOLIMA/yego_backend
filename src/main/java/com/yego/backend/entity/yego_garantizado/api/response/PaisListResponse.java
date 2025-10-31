package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta simplificada para listado de países
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaisListResponse {
    
    private Long id;
    private String nombre;
}

