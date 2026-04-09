package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;
import com.yego.backend.repository.yego_ticketerera.QueueTicketHistoryRepository;
import com.yego.backend.service.yego_ticketerera.QueueTicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de historial de tickets del sistema YEGO Ticketerera
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueTicketHistoryServiceImpl implements QueueTicketHistoryService {
    
    private final QueueTicketHistoryRepository queueTicketHistoryRepository;
    
    @Override
    @Transactional
    public QueueTicketHistory registrarCambioEstado(
            Long ticketId,
            Long agentId,
            String previousStatus,
            String newStatus,
            String notes) {
        
        log.info("Historial ticket {}: {} -> {}", 
                 ticketId, previousStatus, newStatus);
        
        QueueTicketHistory history = QueueTicketHistory.builder()
                .ticketId(ticketId)
                .agentId(agentId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .notes(notes)
                .build();
        // No es necesario setear createdAt porque @PrePersist lo hace automáticamente
        
        QueueTicketHistory savedHistory = queueTicketHistoryRepository.save(history);
        log.info("Historial registrado id {}", savedHistory.getId());
        
        return savedHistory;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QueueTicketHistory> obtenerHistorialPorTicket(Long ticketId) {
        log.debug("Obteniendo historial para ticket: {}", ticketId);
        return queueTicketHistoryRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }
}