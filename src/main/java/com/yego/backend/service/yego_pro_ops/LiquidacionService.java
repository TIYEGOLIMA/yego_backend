package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionSemanalResponse;

import java.time.LocalDate;
import java.util.Map;

public interface LiquidacionService {

    LiquidacionSemanalResponse getLiquidacionSemanal(String driverId, LocalDate weekStart);

    Map<String, Object> liquidarSemana(String driverId);
}
