package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.response.*;

import java.util.List;

/**
 * Interfaz del servicio de reportes del sistema YEGO Principal
 */
public interface ReportService {
    
    /**
     * Obtener estadísticas del sistema
     */
    SystemStatsDto getSystemStats(Integer days);
    
    /**
     * Obtener datos del dashboard
     */
    DashboardDataDto getDashboardData();
    
    /**
     * Obtener estadísticas de usuarios
     */
    List<UserStatsDto> getUserStats();
    
    /**
     * Obtener estadísticas semanales
     */
    WeeklyStatsDto getWeeklyStats();
    
    /**
     * Exportar reporte
     */
    byte[] exportReport(String type, Integer days);
}