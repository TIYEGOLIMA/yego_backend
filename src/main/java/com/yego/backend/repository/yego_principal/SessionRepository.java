package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA para sesiones del sistema YEGO Principal
 * Equivalente a SessionRepository de TypeORM
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Buscar sesiones activas por usuario
     */
    @Query("SELECT s FROM Session s WHERE s.userId = :userId AND s.active = true")
    List<Session> findActiveSessionsByUserId(@Param("userId") Long userId);

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
     * Buscar sesiones creadas después de una fecha
     */
    List<Session> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);

    /**
     * Todas las sesiones activas ordenadas por fecha de creación (para vista admin)
     */
    Page<Session> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Sesiones activas con búsqueda por usuarios (userIds) o por IP, dispositivo, ciudad, país
     */
    @Query("SELECT s FROM Session s WHERE s.active = true AND (s.userId IN :userIds OR LOWER(COALESCE(s.device,'')) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(COALESCE(s.city,'')) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(COALESCE(s.country,'')) LIKE LOWER(CONCAT('%',:search,'%'))) ORDER BY s.createdAt DESC")
    Page<Session> findByActiveTrueAndSearch(@Param("userIds") List<Long> userIds, @Param("search") String search, Pageable pageable);

    /**
     * Desactivar varias sesiones por IDs en una sola consulta
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Session s SET s.active = false WHERE s.id IN :ids")
    int deactivateByIdIn(@Param("ids") List<Long> ids);
}

