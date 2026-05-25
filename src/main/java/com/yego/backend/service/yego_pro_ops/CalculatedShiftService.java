package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverPaymentSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.FechasConTiposTurnoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.PaidShiftsResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.BillingConfigResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.ResumenSemanalResponse;
import com.yego.backend.entity.yego_pro_ops.entities.CalculatedShift;
import com.yego.backend.entity.yego_pro_ops.entities.FacturacionSemanal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CalculatedShiftService {

    void procesarHorasTurnoDiaAnterior();

    FechasConTiposTurnoResponse obtenerFechasConTiposTurno(String driverId);

    DriverPaymentSummaryResponse obtenerResumenPagos(String fecha);

    PaidShiftsResponse obtenerTurnosPagados(String fecha);

    List<CalculatedShift> obtenerOCalcularTurnos(String driverId, String fecha);

    CompletableFuture<List<CalculatedShift>> calcularTurnosAsync(String driverId, String fecha);

    CompletableFuture<List<CalculatedShift>> recalcularTurnos(String driverId, String fecha);

    void invalidarCacheDetalle(String fecha);

    ResumenSemanalResponse obtenerResumenSemanal(String fechaInicio, String fechaFin);

    FacturacionSemanal registrarFacturacionSemanal(FacturacionSemanal facturacion);

    List<FacturacionSemanal> obtenerHistorialFacturacion(String fechaInicio, String fechaFin);

    BillingConfigResponse obtenerConfiguracionBilling();

    BillingConfigResponse guardarConfiguracionBilling(BillingConfigResponse config, Long userId);
}
