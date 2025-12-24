package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverKpiResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.WorkRulesResponse;

import java.util.List;

public interface FleetDriverService {
    DriverKpiResponse consultarConductores();
    DriverKpiResponse obtenerKpisActuales();
    DriverListResponse obtenerListaConductores(List<String> workRuleIds);
    WorkRulesResponse obtenerReglasTrabajo();
    
    /**
     * Obtiene todos los conductores con status "in_order" y sus detalles
     * @return Respuesta con lista de conductores en orden y sus detalles
     */
    DriversInOrderResponse obtenerConductoresEnOrden();
}

