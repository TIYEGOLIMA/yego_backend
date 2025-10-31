package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para ubicaciones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UbicacionResponse {
    
    private Long id;
    private String nombre;
    private String moneda;
    private String simbolo_moneda;
    private Long parent_id;
    private String nivel;
    private Boolean activo;
}

