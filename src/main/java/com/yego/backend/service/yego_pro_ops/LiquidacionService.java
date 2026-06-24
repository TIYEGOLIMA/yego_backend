package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.LiquidarRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionPendienteResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionSemanalResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public interface LiquidacionService {

    LiquidacionSemanalResponse getLiquidacionSemanal(String driverId, LocalDate weekStart);

    LiquidacionPendienteResponse getLiquidacionPendiente(String driverId, LocalDateTime desde, LocalDateTime hasta);

    Map<String, Object> liquidarPendiente(LiquidarRequest request);

    void limpiarFacturacion(String driverId, LocalDate desde, LocalDate hasta);

    /**
     * Producido bruto (tarjeta + efectivo + propinas/promos) consultado a Yango
     * para la ventana de tiempo indicada. Devuelve ZERO si Yango no responde o no
     * hay viajes en el período.
     */
    BigDecimal calcularProducidoYango(String driverId, LocalDateTime desde, LocalDateTime hasta);

    /**
     * Recalcula y rellena monto_total_producido en los cierres históricos que están en NULL,
     * consultando Yango por la ventana de cada sesión. Idempotente y tolerante a fallos.
     * Devuelve un resumen { procesados, actualizados, fallidos }.
     */
    Map<String, Object> backfillProducido();
}
