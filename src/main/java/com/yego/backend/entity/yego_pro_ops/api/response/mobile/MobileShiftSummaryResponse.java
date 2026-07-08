package com.yego.backend.entity.yego_pro_ops.api.response.mobile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileShiftSummaryResponse {

    private int viajes;
    private BigDecimal producido;
    private BigDecimal efectivo;
    private BigDecimal yape;
    private String duracion;
    private Integer kmInicial;
}
