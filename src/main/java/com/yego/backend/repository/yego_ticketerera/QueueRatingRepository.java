package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para la entidad QueueRating del sistema YEGO Ticketerera
 */
@Repository
public interface QueueRatingRepository extends JpaRepository<QueueRating, Long> {
    
    List<QueueRating> findByTicketId(Long ticketId);
    
    List<QueueRating> findByAgentId(Long agentId);
    
    // Obtener calificaciones ordenadas por fecha de creación (más recientes primero)
    List<QueueRating> findByAgentIdOrderByCreatedAtDesc(Long agentId);
    
    // Obtener todas las calificaciones ordenadas por fecha de creación
    List<QueueRating> findAllByOrderByCreatedAtDesc();
    
    // Contar calificaciones por agente
    long countByAgentId(Long agentId);
    
    // Obtener calificaciones recientes (últimas N)
    @Query("SELECT qr FROM QueueRating qr ORDER BY qr.createdAt DESC")
    List<QueueRating> findRecentRatings(org.springframework.data.domain.Pageable pageable);
}
