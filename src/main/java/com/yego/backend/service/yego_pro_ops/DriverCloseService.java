package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.DriverCloseRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;

import java.util.Optional;

public interface DriverCloseService {
    DriverClose registrarCierre(DriverCloseRequest request);
    Optional<DriverCloseResponse> obtenerCierrePorDriverIdYFecha(String driverId, String fecha);
    DriverClose actualizarCierre(DriverCloseRequest request);
}

