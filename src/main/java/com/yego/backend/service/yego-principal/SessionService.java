package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.entity.yego_principal.entities.ConnectionLog;
import com.yego.backend.entity.yego_principal.entities.Session;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Interfaz del servicio de sesiones del sistema YEGO Principal
 * Equivalente a SessionsService de NestJS
 */
public interface SessionService {
    
    /**
     * Crear nueva sesión
     */
    SessionResponseDto create(CreateSessionDto createSessionDto, Long userId, HttpServletRequest request);
    
    /**
     * Obtener todas las sesiones activas
     */
    List<SessionResponseDto> findAll(Long userId);
    
    /**
     * Obtener sesión por ID
     */
    SessionResponseDto findOne(Long id);
    
    /**
     * Buscar sesión por token hash
     */
    Session findByTokenHash(String tokenHash);
    
    /**
     * Desactivar sesión
     */
    void deactivate(Long id);
    
    /**
     * Desactivar todas las sesiones de un usuario
     */
    void deactivateByUserId(Long userId, String reason);
    
    /**
     * Desactivar sesión por token hash
     */
    void deactivateByTokenHash(String tokenHash);
    
    /**
     * Limpiar sesiones expiradas
     */
    Integer cleanupExpiredSessions();
    
    /**
     * Obtener cantidad de sesiones activas
     */
    Long getActiveSessionsCount(Long userId);
    
    /**
     * Obtener estadísticas de sesiones
     */
    SessionStatsDto getSessionStats();
    
    /**
     * Obtener estadísticas de WebSocket
     */
    ConnectionStatsDto getWebSocketStats();
    
    /**
     * Obtener sesiones activas de WebSocket
     */
    List<SessionDataDto> getWebSocketSessions();
    
    /**
     * Obtener logs de conexión
     */
    List<ConnectionLogResponseDto> getConnectionLogs(Integer days, Integer limit, Long userId, String roleName);
    
    /**
     * Forzar cierre de sesión
     */
    void forceLogout(Long sessionId, Long adminUserId);
}
