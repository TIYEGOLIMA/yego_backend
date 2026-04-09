package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para la entidad Ticket del sistema YEGO Ticketerera
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    List<Ticket> findByStatusOrderByCreatedAtAsc(TicketStatus status);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status")
    long countByStatus(@Param("status") TicketStatus status);
    
    @Query("SELECT t FROM Ticket t WHERE t.status IN ('WAITING', 'CALLED', 'IN_PROGRESS') ORDER BY t.priority DESC, t.createdAt ASC")
    List<Ticket> findActiveTickets();
    
    boolean existsByTicketNumber(String ticketNumber);
    
    @Query("SELECT t FROM Ticket t WHERE t.moduleId = :moduleId AND t.status = :status ORDER BY t.priority DESC, t.createdAt ASC")
    List<Ticket> findWaitingTicketsByModuleOrdered(@Param("moduleId") Long moduleId, @Param("status") TicketStatus status);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.moduleId = :moduleId AND t.status = :status")
    long countByModuleIdAndStatus(@Param("moduleId") Long moduleId, @Param("status") TicketStatus status);
    
    // Buscar tickets ya asignados al agente
    List<Ticket> findByAgentIdAndStatusIn(Long agentId, List<TicketStatus> statuses);
    
    // Buscar el próximo ticket disponible (sin agente asignado)
    @Query("SELECT t FROM Ticket t WHERE t.agentId IS NULL AND t.status = :status ORDER BY t.createdAt ASC")
    Optional<Ticket> findFirstAvailableTicket(@Param("status") TicketStatus status);
    
    // Consulta optimizada: Obtener tickets por múltiples usuarios en una sola query
    List<Ticket> findByUserIdIn(List<Long> userIds);
    
    // Consulta optimizada: Obtener tickets por múltiples usuarios con filtro de fecha
    @Query("SELECT t FROM Ticket t WHERE t.userId IN :userIds AND t.createdAt >= :fechaInicio AND t.createdAt <= :fechaFin")
    List<Ticket> findByUserIdInAndCreatedAtBetween(@Param("userIds") List<Long> userIds, 
                                                     @Param("fechaInicio") java.time.LocalDateTime fechaInicio, 
                                                     @Param("fechaFin") java.time.LocalDateTime fechaFin);
}
