package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para sesiones del sistema YEGO Principal
 * Equivalente a SessionRepository de TypeORM
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    /**
     * Buscar sesión por sessionId
     */
    Optional<Session> findBySessionId(String sessionId);
    
    /**
     * Buscar sesión por socketId
     */
    Optional<Session> findBySocketId(String socketId);
    
    /**
     * Buscar sesiones activas por usuario
     */
    @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.isActive = true")
    List<Session> findActiveSessionsByUserId(@Param("userId") Long userId);
    
    /**
     * Buscar todas las sesiones por usuario
     */
    @Query("SELECT s FROM Session s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<Session> findByUserId(@Param("userId") Long userId);
    
    /**
     * Contar sesiones activas
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.isActive = true")
    Long countActiveSessions();
    
    /**
     * Contar sesiones creadas después de una fecha
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.createdAt > :date")
    Long countSessionsCreatedAfter(@Param("date") LocalDateTime date);
    
    /**
     * Buscar sesiones inactivas para limpieza
     */
    @Query("SELECT s FROM Session s WHERE s.lastActivity < :cutoffTime AND s.isActive = true")
    List<Session> findInactiveSessionsForCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);
}
