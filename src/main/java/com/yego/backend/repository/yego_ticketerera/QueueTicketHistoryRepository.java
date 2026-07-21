package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;

/**
 * Repositorio para QueueTicketHistory del sistema YEGO Ticketerera
 */
@Repository
public interface QueueTicketHistoryRepository extends JpaRepository<QueueTicketHistory, Long> {
    
    /**
     * Buscar historial por ticket ID
     */
    List<QueueTicketHistory> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    List<QueueTicketHistory> findByTicketIdInOrderByCreatedAtAsc(Collection<Long> ticketIds);
}
