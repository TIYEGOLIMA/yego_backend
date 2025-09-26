package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.repository.yego_ticketerera.QueueTicketHistoryRepository;
import com.yego.backend.service.yego_ticketerera.QueueTicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de QueueTicketHistory del sistema YEGO Ticketerera
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueTicketHistoryServiceImpl implements QueueTicketHistoryService {
    
    private final QueueTicketHistoryRepository queueTicketHistoryRepository;
    
    @Override
    @Transactional
    public QueueTicketHistory registrarCambioEstado(Long ticketId, Long agentId, 
                                                   String estadoAnterior, String nuevoEstado, 
                                                   String notas) {
        log.info("Registrando cambio de estado para ticket {}: {} -> {}", 
                ticketId, estadoAnterior, nuevoEstado);
        
        QueueTicketHistory historial = QueueTicketHistory.builder()
                .ticketId(ticketId)
                .agentId(agentId)
                .previousStatus(estadoAnterior)
                .newStatus(nuevoEstado)
                .notes(notas)
                .build();
        
        QueueTicketHistory saved = queueTicketHistoryRepository.save(historial);
        log.info("Historial registrado con ID: {}", saved.getId());
        
        return saved;
    }
    
    @Override
    @Transactional
    public QueueTicketHistory registrarTicketCompletado(Ticket ticket, Long agentId, String notas) {
        return registrarCambioEstado(
            ticket.getId(),
            agentId,
            ticket.getStatus().name(),
            "COMPLETED",
            notas
        );
    }
    
    @Override
    public List<QueueTicketHistory> obtenerHistorialPorTicket(Long ticketId) {
        log.info("Obteniendo historial para ticket: {}", ticketId);
        return queueTicketHistoryRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }
    
    @Override
    public List<QueueTicketHistory> obtenerHistorialPorAgente(Long agentId) {
        log.info("Obteniendo historial para agente: {}", agentId);
        return queueTicketHistoryRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
    }
}
