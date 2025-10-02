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
    
    @Query("SELECT t FROM Ticket t WHERE t.status = 'CALLED' ORDER BY t.calledAt DESC LIMIT 1")
    Optional<Ticket> findLastCalledTicket();
    
    @Query("SELECT t FROM Ticket t WHERE t.status IN ('WAITING', 'CALLED', 'IN_PROGRESS') ORDER BY t.priority DESC, t.createdAt ASC")
    List<Ticket> findActiveTickets();
    
    boolean existsByTicketNumber(String ticketNumber);
    
    @Query("SELECT t FROM Ticket t WHERE t.moduleId = :moduleId AND t.status = :status ORDER BY t.priority DESC, t.createdAt ASC")
    List<Ticket> findWaitingTicketsByModuleOrdered(@Param("moduleId") Long moduleId, @Param("status") TicketStatus status);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.moduleId = :moduleId AND t.status = :status")
    long countByModuleIdAndStatus(@Param("moduleId") Long moduleId, @Param("status") TicketStatus status);
    
    // Contar todos los tickets de un módulo (para número consecutivo)
    long countByModuleId(Long moduleId);
    
    // Obtener tickets por módulo con información completa
    @Query(value = """
        SELECT t.id AS id,
               t.ticket_number AS ticketNumber,
               t.status AS status,
               t.created_at AS createdAt,
               t.priority AS priority,
               u.name AS createdBy,
               t.license_number AS phone,
               m.name AS moduleName,
               parent_o.name AS categoryName,
               o.name AS subcategoryName
        FROM tickets t
        JOIN users u ON u.id = t.user_id
        JOIN yego_modules m ON m.id = u.module_id
        LEFT JOIN options o ON o.id = t.option_id
        LEFT JOIN options parent_o ON parent_o.id = o.parent_id
                 WHERE u.role IN ('SUPERADMIN', 'TV')
           AND u.module_id = :moduleId
          AND t.status IN ('WAITING','IN_PROGRESS','CALLED')
    """, nativeQuery = true)
    List<Object[]> findTicketsByModule(@Param("moduleId") Long moduleId);
    
    // Para asignación automática de tickets
    
    // Buscar tickets ya asignados al agente
    List<Ticket> findByAgentIdAndStatusIn(Long agentId, List<TicketStatus> statuses);
    
    // Buscar el próximo ticket disponible (sin agente asignado)
    @Query("SELECT t FROM Ticket t WHERE t.agentId IS NULL AND t.status = :status ORDER BY t.createdAt ASC")
    Optional<Ticket> findFirstAvailableTicket(@Param("status") TicketStatus status);
    
    // Buscar tickets por agente (para estadísticas de SAC)
    List<Ticket> findByAgentId(Long agentId);
    
    // Buscar tickets por estado específico
    List<Ticket> findByStatus(TicketStatus status);
    
    // Contar tickets por agente
    long countByAgentId(Long agentId);
    
    // Contar tickets completados por agente
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.agentId = :agentId AND t.status = 'COMPLETED'")
    long countCompletedTicketsByAgentId(@Param("agentId") Long agentId);
}
