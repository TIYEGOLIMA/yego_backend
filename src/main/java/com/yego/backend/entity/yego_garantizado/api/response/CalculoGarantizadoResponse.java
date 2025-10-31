package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta para cálculos de garantizado
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculoGarantizadoResponse {
    
    private Long id;
    private String pais;
    private String ciudad;
    private String semana;
    
    // Con brandeo
    private Integer viajesConBrandeo;
    private BigDecimal bonoConBrandeo;
    private BigDecimal garantizadoConBrandeo;
    private Integer horasConBrandeo;
    
    // Sin brandeo
    private Integer viajesSinBrandeo;
    private BigDecimal bonoSinBrandeo;
    private BigDecimal garantizadoSinBrandeo;
    private Integer horasSinBrandeo;
    
    private Boolean activo;
}

