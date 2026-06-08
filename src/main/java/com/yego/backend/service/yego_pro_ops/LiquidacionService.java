package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.LiquidarRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionPendienteResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionSemanalResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public interface LiquidacionService {

    LiquidacionSemanalResponse getLiquidacionSemanal(String driverId, LocalDate weekStart);

    LiquidacionPendienteResponse getLiquidacionPendiente(String driverId, LocalDateTime desde, LocalDateTime hasta);

    Map<String, Object> liquidarPendiente(LiquidarRequest request);

    void limpiarFacturacion(String driverId, LocalDate desde, LocalDate hasta);
}
