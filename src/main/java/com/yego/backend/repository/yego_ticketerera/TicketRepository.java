package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    List<Ticket> findByAgentIdAndStatusIn(Long agentId, List<TicketStatus> statuses);

    @Query("SELECT t FROM Ticket t WHERE t.agentId IS NULL AND t.status = :status ORDER BY t.createdAt ASC")
    Optional<Ticket> findFirstAvailableTicket(@Param("status") TicketStatus status);

    List<Ticket> findByUserIdIn(List<Long> userIds);

    @Query("SELECT t FROM Ticket t WHERE t.userId IN :userIds AND t.createdAt >= :fechaInicio AND t.createdAt <= :fechaFin")
    List<Ticket> findByUserIdInAndCreatedAtBetween(@Param("userIds") List<Long> userIds,
                                                   @Param("fechaInicio") java.time.LocalDateTime fechaInicio,
                                                   @Param("fechaFin") java.time.LocalDateTime fechaFin);

    List<Ticket> findBySedeIdAndStatusOrderByCreatedAtAsc(Long sedeId, TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.sedeId = :sedeId AND t.status = :status")
    long countBySedeIdAndStatus(@Param("sedeId") Long sedeId, @Param("status") TicketStatus status);

    @Query("SELECT t FROM Ticket t WHERE t.sedeId = :sedeId AND t.status IN ('WAITING', 'CALLED', 'IN_PROGRESS') ORDER BY t.priority DESC, t.createdAt ASC")
    List<Ticket> findActiveTicketsBySede(@Param("sedeId") Long sedeId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.sedeId = :sedeId")
    long countBySedeId(@Param("sedeId") Long sedeId);
}
