package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response completo para lista de conductores garantizados
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GarantizadoListResponse {
    
    private String semanaActual; 
    private List<GarantizadoResponse> conductores;
}
