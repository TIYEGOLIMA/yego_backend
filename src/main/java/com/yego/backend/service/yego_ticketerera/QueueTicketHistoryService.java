package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;

import java.util.List;

/**
 * Interface del servicio de QueueTicketHistory del sistema YEGO Ticketerera
 */
public interface QueueTicketHistoryService {
    
    QueueTicketHistory registrarCambioEstado(Long ticketId, Long agentId, 
                                           String estadoAnterior, String nuevoEstado, 
                                           String notas);
    
    QueueTicketHistory registrarTicketCompletado(Ticket ticket, Long agentId, String notas);
    
    List<QueueTicketHistory> obtenerHistorialPorTicket(Long ticketId);
    
    List<QueueTicketHistory> obtenerHistorialPorAgente(Long agentId);
}
