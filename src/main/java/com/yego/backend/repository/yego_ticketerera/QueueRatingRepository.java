package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    // Consulta optimizada: Obtener calificaciones por múltiples tickets en una sola query
    @Query("SELECT qr FROM QueueRating qr WHERE qr.ticketId IN :ticketIds")
    List<QueueRating> findByTicketIdIn(@Param("ticketIds") List<Long> ticketIds);
    
    // Consulta optimizada: Obtener promedio y total de calificaciones por agente
    @Query("SELECT AVG(qr.score), COUNT(qr) FROM QueueRating qr WHERE qr.agentId = :agentId")
    Object[] getAverageAndCountByAgentId(@Param("agentId") Long agentId);
    
    // Consulta optimizada: Obtener promedio general de todas las calificaciones
    @Query("SELECT AVG(qr.score) FROM QueueRating qr")
    Double getAverageRating();
    
    // Consulta optimizada: Obtener calificaciones recientes con información de ticket y agente
    @Query(value = """
        SELECT qr.id, qr.score, qr.comment, qr.created_at, qr.ticket_id, qr.agent_id,
               t.ticket_number, u.name as sac_name
        FROM queue_ratings qr
        LEFT JOIN tickets t ON t.id = qr.ticket_id
        LEFT JOIN queue_agents qa ON qa.id = qr.agent_id
        LEFT JOIN users u ON u.id = qa.user_id
        ORDER BY qr.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findRecentRatingsWithDetails(@Param("limit") int limit);
}
