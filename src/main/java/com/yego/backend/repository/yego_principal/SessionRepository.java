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
     * Buscar sesión por tokenHash
     */
    Optional<Session> findByTokenHash(String tokenHash);
    
    /**
     * Buscar sesiones activas por usuario
     */
    @Query("SELECT s FROM Session s WHERE s.userId = :userId AND s.active = true")
    List<Session> findActiveSessionsByUserId(@Param("userId") Long userId);
    
    /**
     * Buscar todas las sesiones por usuario
     */
    @Query("SELECT s FROM Session s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<Session> findByUserId(@Param("userId") Long userId);
    
    /**
     * Contar sesiones activas
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.active = true")
    Long countActiveSessions();
    
    /**
     * Contar sesiones creadas después de una fecha
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.createdAt > :date")
    Long countSessionsCreatedAfter(@Param("date") LocalDateTime date);
    
    /**
     * Buscar sesiones inactivas para limpieza
     */
    @Query("SELECT s FROM Session s WHERE s.expiresAt < :cutoffTime AND s.active = true")
    List<Session> findInactiveSessionsForCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Buscar sesiones creadas después de una fecha
     */
    List<Session> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);
}

