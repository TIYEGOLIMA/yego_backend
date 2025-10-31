package com.yego.backend.entity.yego_garantizado.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para request completo del cálculo de garantizado desde el frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalcularGarantizadoRequest {
    
    private List<PaisCalculoRequest> paises;
    private String semana; // Semana a calcular
}

