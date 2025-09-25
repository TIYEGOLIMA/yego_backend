package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;

import java.util.List;
import java.util.Map;

/**
 * Interfaz del servicio de auditoría del sistema YEGO Principal
 * Equivalente a AuditService de NestJS
 */
public interface AuditService {
    
    /**
     * Crear log de auditoría
     */
    AuditLogResponseDto create(CreateAuditLogDto createAuditLogDto, Long userId);
    
    /**
     * Obtener logs con filtros y paginación
     */
    AuditLogPageDto findAll(Integer page, Integer limit, AuditFilterDto filters);
    
    /**
     * Obtener log por ID
     */
    AuditLogResponseDto findOne(Long id);
    
    /**
     * Obtener logs por usuario
     */
    List<AuditLogResponseDto> findByUser(Long userId, Integer limit);
    
    /**
     * Obtener logs por acción
     */
    List<AuditLogResponseDto> findByAction(String action, Integer limit);
    
    /**
     * Obtener logs por recurso
     */
    List<AuditLogResponseDto> findByResource(String resource, Integer limit);
    
    /**
     * Obtener estadísticas de auditoría
     */
    AuditStatsDto getStats(Integer days);
    
    /**
     * Obtener actividad reciente
     */
    List<AuditLogResponseDto> getRecentActivity(Integer limit);
    
    /**
     * Registrar login exitoso
     */
    void logLogin(Long userId, String ipAddress, String userAgent);
    
    /**
     * Registrar logout
     */
    void logLogout(Long userId, String ipAddress, String userAgent);
    
    /**
     * Registrar login fallido
     */
    void logFailedLogin(String username, String ipAddress, String userAgent);
    
    /**
     * Registrar acción de usuario
     */
    void logUserAction(Long userId, String action, String resource, String resourceId, 
                      String details, String ipAddress, String userAgent);
    
    /**
     * Registrar acción del sistema
     */
    void logSystemAction(String action, String resource, String resourceId, String details);
}
