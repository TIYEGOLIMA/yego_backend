package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.BillingConfigResponse;
import com.yego.backend.entity.yego_pro_ops.entities.FacturacionSemanal;

import java.util.List;

public interface FacturacionSemanalService {

    FacturacionSemanal registrarFacturacionSemanal(FacturacionSemanal facturacion);

    List<FacturacionSemanal> obtenerHistorialFacturacion(String fechaInicio, String fechaFin);

    BillingConfigResponse obtenerConfiguracionBilling();

    BillingConfigResponse guardarConfiguracionBilling(BillingConfigResponse config, Long userId);
}
