package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;

/**
 * Servicio para estadísticas de SAC (Servicio al Cliente)
 */
public interface SacStatsService {

    /**
     * Calcula las estadísticas SAC. Cualquiera de los parámetros puede ser {@code null}:
     * sin fechas se consideran todos los registros y sin {@code sedeId} se incluyen todas las sedes.
     */
    SacStatsResponse obtenerTodasLasEstadisticas(String fechaInicio, String fechaFin, Long sedeId);
}
