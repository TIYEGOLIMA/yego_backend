package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.DriverCloseRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;

import java.util.Optional;
import java.util.UUID;

public interface DriverCloseService {

    DriverClose registrarCierre(DriverCloseRequest request);

    Optional<DriverCloseResponse> obtenerCierrePorDriverIdYFecha(String driverId, String fecha);

    Optional<DriverCloseResponse> obtenerCierrePorSessionId(UUID sessionId);

    DriverClose actualizarCierre(DriverCloseRequest request);
}
