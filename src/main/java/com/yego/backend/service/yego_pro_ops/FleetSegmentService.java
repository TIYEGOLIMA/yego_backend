package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.FleetSegmentResponse;

import java.util.List;
import java.util.UUID;

public interface FleetSegmentService {

    /** Lista las flotas activas con nombre resuelto y conteo de vehículos. */
    List<FleetSegmentResponse> listarFlotas();

    /** Registra una nueva flota por park_id. */
    FleetSegmentResponse agregarFlota(String parkId, Long createdById);

    /** Baja lógica (soft delete) de una flota. */
    void desactivarFlota(UUID id);
}
