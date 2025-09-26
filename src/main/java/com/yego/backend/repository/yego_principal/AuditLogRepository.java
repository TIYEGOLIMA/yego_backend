package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad AuditLog del sistema YEGO Principal
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * Buscar logs por usuario
     */
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Buscar logs por acción
     */
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
    
    /**
     * Buscar logs por recurso
     */
    List<AuditLog> findByResourceOrderByCreatedAtDesc(String resource, Pageable pageable);
    
    /**
     * Buscar logs recientes
     */
    List<AuditLog> findTop10ByOrderByCreatedAtDesc();
    
    /**
     * Buscar logs con límite específico
     */
    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentActivity(Pageable pageable);
    
    /**
     * Contar logs por acción después de una fecha
     */
    Long countByActionAndCreatedAtAfter(String action, LocalDateTime date);
    
    /**
     * Contar logs después de una fecha
     */
    Long countByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Buscar logs por rango de fechas
     */
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Buscar logs por usuario después de una fecha
     */
    List<AuditLog> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime date);
    
    /**
     * Estadísticas por acción en un rango de fechas
     */
    @Query("SELECT a.action as action, COUNT(a) as count FROM AuditLog a WHERE a.createdAt >= :startDate GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> getActionStats(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Estadísticas por recurso en un rango de fechas
     */
    @Query("SELECT a.resource as resource, COUNT(a) as count FROM AuditLog a WHERE a.createdAt >= :startDate AND a.resource IS NOT NULL GROUP BY a.resource ORDER BY COUNT(a) DESC")
    List<Object[]> getResourceStats(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Estadísticas por usuario en un rango de fechas
     */
    @Query("SELECT a.userId as userId, COUNT(a) as count FROM AuditLog a WHERE a.createdAt >= :startDate AND a.userId IS NOT NULL GROUP BY a.userId ORDER BY COUNT(a) DESC")
    List<Object[]> getUserStats(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Estadísticas diarias en un rango de fechas
     */
    @Query("SELECT DATE(a.createdAt) as date, COUNT(a) as count FROM AuditLog a WHERE a.createdAt >= :startDate GROUP BY DATE(a.createdAt) ORDER BY DATE(a.createdAt) ASC")
    List<Object[]> getDailyStats(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Búsqueda compleja con filtros múltiples
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:resource IS NULL OR a.resource = :resource) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) AND " +
           "(:search IS NULL OR a.action LIKE %:search% OR a.resource LIKE %:search% OR a.resourceId LIKE %:search%) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findWithFilters(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("resource") String resource,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            Pageable pageable
    );
    
    /**
     * Buscar logs creados después de una fecha
     */
    List<AuditLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);
}

