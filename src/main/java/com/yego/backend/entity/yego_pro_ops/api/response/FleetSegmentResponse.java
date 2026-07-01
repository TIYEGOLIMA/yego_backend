package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Flota registrada para el listado / selector del frontend.
 * El nombre y ciudad se resuelven desde la API de partners a partir del parkId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetSegmentResponse {
    private UUID id;
    private String parkId;
    private String nombre;
    private String ciudad;
    private Boolean activo;
    private Long totalVehiculos;
}
