package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Interfaz del servicio de sesiones del sistema YEGO Principal
 */
public interface SessionService {
    
    /**
     * Crear nueva sesión
     */
    SessionResponseDto create(CreateSessionDto createSessionDto, Long userId, HttpServletRequest request);
    
    /**
     * Obtener sesiones por usuario (userId null = todas las activas para admin)
     */
    List<SessionResponseDto> findAll(Long userId);

    /**
     * Sesiones activas paginadas (para admin). search opcional: usuario, email, IP, dispositivo, ciudad, país
     */
    SessionPageDto findActiveSessionsPage(int page, int size, String search);

    /**
     * Desactivar varias sesiones por IDs
     */
    void deactivateByIds(List<Long> ids);

    /**
     * Desactivar sesiones por usuario
     */
    void deactivateByUserId(Long userId, String reason);

    /**
     * Obtener estadísticas de sesiones
     */
    SessionStatsDto getSessionStats();

    /**
     * Obtener logs de conexión
     */
    List<ConnectionLogResponseDto> getConnectionLogs(Integer days, Integer limit, Long userId, String roleName);
    
    /**
     * Forzar logout de sesión
     */
    void forceLogout(Long sessionId, Long adminUserId);
}