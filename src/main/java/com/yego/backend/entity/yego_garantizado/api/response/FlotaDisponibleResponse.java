package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta con información del conductor y sus flotas disponibles
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlotaDisponibleResponse {
    private String licenseNumber;
    private DriverInfo conductor;
    private List<FlotaInfo> flotasDisponibles;
    private String mensaje;
}

