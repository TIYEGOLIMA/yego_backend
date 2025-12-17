package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverKpiResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.WorkRulesResponse;

import java.util.List;

public interface FleetDriverService {
    DriverKpiResponse consultarConductores();
    DriverKpiResponse obtenerKpisActuales();
    DriverListResponse obtenerListaConductores(List<String> workRuleIds);
    WorkRulesResponse obtenerReglasTrabajo();
}

