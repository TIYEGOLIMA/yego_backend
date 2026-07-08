package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleMobileResponse {
    private String yangoCarId;
    private String placa;
    private String marca;
    private String modelo;
    private Integer anio;
    private String estado;
    private String flota;
    private String imagen;
}
