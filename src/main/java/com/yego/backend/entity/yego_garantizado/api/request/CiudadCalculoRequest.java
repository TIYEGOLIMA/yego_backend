package com.yego.backend.entity.yego_garantizado.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para ciudad con sus cálculos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CiudadCalculoRequest {
    
    private String ciudad;
    private List<CalculoRequest> conBrandeo;
    private List<CalculoRequest> sinBrandeo;
}

