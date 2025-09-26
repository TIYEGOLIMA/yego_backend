package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearRatingRequest;
import com.yego.backend.repository.yego_ticketerera.QueueRatingRepository;
import com.yego.backend.repository.yego_ticketerera.TicketRepository;
import com.yego.backend.service.yego_ticketerera.QueueRatingService;
import com.yego.backend.service.yego_ticketerera.QueueTicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de QueueRating del sistema YEGO Ticketerera
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueRatingServiceImpl implements QueueRatingService {
    
    private final QueueRatingRepository queueRatingRepository;
    private final TicketRepository ticketRepository;
    private final QueueTicketHistoryService queueTicketHistoryService;
    
    @Override
    public QueueRating crearRating(CrearRatingRequest request) {
        log.info("📝 [QueueRatingService] Creando rating para ticket: {}, score: {}", 
                request.getTicketId(), request.getScore());
        
        // Verificar que el ticket existe
        Optional<Ticket> ticketOpt = ticketRepository.findById(request.getTicketId());
        if (ticketOpt.isEmpty()) {
            throw new RuntimeException("Ticket no encontrado con ID: " + request.getTicketId());
        }
        
        // Obtener el agente desde el historial de tickets completados
        List<QueueTicketHistory> historial = queueTicketHistoryService.obtenerHistorialPorTicket(request.getTicketId());
        
        // Buscar el último registro donde el ticket fue completado
        Optional<QueueTicketHistory> completadoOpt = historial.stream()
                .filter(h -> "COMPLETED".equals(h.getNewStatus()))
                .findFirst();
        
        if (completadoOpt.isEmpty()) {
            throw new RuntimeException("El ticket no ha sido completado aún");
        }
        
        Long agentId = completadoOpt.get().getAgentId();
        log.info("🔍 [QueueRatingService] Agente encontrado: {} para ticket: {}", agentId, request.getTicketId());
        
        QueueRating rating = QueueRating.builder()
                .ticketId(request.getTicketId())
                .agentId(agentId)
                .score(request.getScore())
                .comment(request.getComment())
                .build();
        
        QueueRating savedRating = queueRatingRepository.save(rating);
        
        log.info("✅ [QueueRatingService] Rating creado exitosamente con ID: {} para agente: {}", 
                savedRating.getId(), agentId);
        return savedRating;
    }
    
    @Override
    public List<QueueRating> obtenerRatingsPorTicket(Long ticketId) {
        log.info("🔍 [QueueRatingService] Obteniendo ratings para ticket: {}", ticketId);
        return queueRatingRepository.findByTicketId(ticketId);
    }
}
