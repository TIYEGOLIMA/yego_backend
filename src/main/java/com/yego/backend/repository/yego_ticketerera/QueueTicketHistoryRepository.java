package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository para la entidad QueueTicketHistory del sistema YEGO Ticketerera
 */
@Repository
public interface QueueTicketHistoryRepository extends JpaRepository<QueueTicketHistory, Long> {
    
    // Buscar historial por ticket
    List<QueueTicketHistory> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
    
    // Buscar historial por agente
    List<QueueTicketHistory> findByAgentIdOrderByCreatedAtDesc(Long agentId);
}
