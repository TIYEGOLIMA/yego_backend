package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para obtener todas las ubicaciones con su jerarquía
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UbicacionesCompletasResponse {
    
    private List<PaisConCiudadesResponse> paises;
}

