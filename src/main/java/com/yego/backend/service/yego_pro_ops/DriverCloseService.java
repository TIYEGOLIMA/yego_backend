package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.DriverCloseRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;

import java.util.Optional;

public interface DriverCloseService {
    /**
     * 📋 VISTA: DetalleView
     * Registra un nuevo cierre de caja para un conductor en una fecha específica
     */
    DriverClose registrarCierre(DriverCloseRequest request);
    
    /**
     * 📋 VISTA: DetalleView
     * Obtiene un cierre de caja existente por driver_id y fecha
     */
    Optional<DriverCloseResponse> obtenerCierrePorDriverIdYFecha(String driverId, String fecha);
    
    /**
     * 📋 VISTA: DetalleView
     * Actualiza un cierre de caja existente
     */
    DriverClose actualizarCierre(DriverCloseRequest request);
}

