package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Card ligero para el buscador de la app móvil (búsqueda por placa).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileVehicleCard {
    private String yangoCarId;
    private String placa;
    private String marca;
    private String modelo;
    private Integer anio;
    private String estado;
    private String flota;
    private String imagen;
}
