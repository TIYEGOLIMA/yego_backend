package com.yego.backend.entity.yego_garantizado.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cada cálculo individual
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculoRequest {
    
    private Integer viajes;
    private Integer bono;
    private Integer garantizado;
    private Integer horas; // horas trabajadas mínimas requeridas
}

