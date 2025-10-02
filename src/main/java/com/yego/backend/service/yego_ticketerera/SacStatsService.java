package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;

/**
 * Servicio para estadísticas de SAC (Servicio al Cliente)
 */
public interface SacStatsService {
    
    /**
     * Obtener TODAS las estadísticas de SAC en una sola respuesta
     * Incluye: estadísticas generales, rendimiento individual, top performers y calificaciones recientes
     */
    SacStatsResponse obtenerTodasLasEstadisticas();
}
