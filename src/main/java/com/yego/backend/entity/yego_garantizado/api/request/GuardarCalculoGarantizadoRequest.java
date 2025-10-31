package com.yego.backend.entity.yego_garantizado.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para guardar cálculos de garantizado
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuardarCalculoGarantizadoRequest {
    
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
}

