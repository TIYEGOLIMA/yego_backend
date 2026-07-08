package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MobileShiftSummaryResponse {
    private Integer viajes;
    private BigDecimal producido;
    private BigDecimal efectivo;
    private BigDecimal yape;
    private String duracion;
    private Integer kmInicial;
}
