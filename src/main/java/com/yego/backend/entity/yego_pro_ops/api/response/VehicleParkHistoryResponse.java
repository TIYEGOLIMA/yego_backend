package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de trazabilidad de cambio de flota (park) de un vehículo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleParkHistoryResponse {
    private UUID id;
    private String yangoCarId;
    private String number;
    private UUID segmentIdAnterior;
    private UUID segmentIdNuevo;
    private String parkIdAnterior;
    private String parkIdNuevo;
    private String flotaAnterior;
    private String flotaNuevo;
    private String tipo;
    private LocalDateTime createdAt;
}
