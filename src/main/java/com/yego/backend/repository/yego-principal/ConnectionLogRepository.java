package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.ConnectionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA para logs de conexión del sistema YEGO Principal
 * Equivalente a ConnectionLogRepository de TypeORM
 */
@Repository
public interface ConnectionLogRepository extends JpaRepository<ConnectionLog, Long> {
    
    /**
     * Buscar logs por usuario
     */
    List<ConnectionLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Buscar logs por sesión
     */
    List<ConnectionLog> findBySessionIdOrderByCreatedAtDesc(Long sessionId);
    
    /**
     * Buscar logs por rango de fechas
     */
    List<ConnectionLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    
    /**
     * Buscar logs con filtros
     */
    @Query("SELECT cl FROM ConnectionLog cl WHERE " +
           "(:userId IS NULL OR cl.userId = :userId) AND " +
           "(:roleName IS NULL OR cl.roleName = :roleName) AND " +
           "cl.createdAt >= :startDate " +
           "ORDER BY cl.createdAt DESC")
    Page<ConnectionLog> findWithFilters(@Param("userId") Long userId, 
                                       @Param("roleName") String roleName,
                                       @Param("startDate") LocalDateTime startDate,
                                       Pageable pageable);
    
    /**
     * Buscar logs recientes
     */
    List<ConnectionLog> findTop50ByOrderByCreatedAtDesc();
}
