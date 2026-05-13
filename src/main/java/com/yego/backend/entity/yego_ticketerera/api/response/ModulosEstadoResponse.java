package com.yego.backend.entity.yego_ticketerera.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de response para el estado de módulos (disponibles y ocupados)
 * en el sistema YEGO Ticketerera
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModulosEstadoResponse {

    private List<ModuloAtencionResponse> modulosDisponibles;
    private List<ModuloOcupadoResponse> modulosOcupados;
    private Long sedeId;
}

