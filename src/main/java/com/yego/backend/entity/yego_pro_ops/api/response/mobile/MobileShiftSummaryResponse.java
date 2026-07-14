package com.yego.backend.entity.yego_pro_ops.api.response.mobile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileShiftSummaryResponse {

    private int viajes;
    private BigDecimal producido;
    private BigDecimal efectivo;
    private BigDecimal yape;
    private BigDecimal tarjeta;
    private BigDecimal corporate;
    private BigDecimal tips;
    private BigDecimal bonos;
    private BigDecimal promocion;
    private BigDecimal distancia;
    private BigDecimal promedioPorViaje;
    private String duracion;
    private Instant fechaInicio;
    private Instant fechaFinPreview;
    private Integer kmInicial;
    private boolean live;
    private String status;
}
