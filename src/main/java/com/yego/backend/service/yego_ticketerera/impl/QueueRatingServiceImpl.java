package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearRatingRequest;
import com.yego.backend.repository.yego_ticketerera.QueueRatingRepository;
import com.yego.backend.repository.yego_ticketerera.TicketRepository;
import com.yego.backend.service.yego_ticketerera.QueueRatingService;
import com.yego.backend.service.yego_ticketerera.QueueTicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueRatingServiceImpl implements QueueRatingService {
    
    private final QueueRatingRepository queueRatingRepository;
    private final TicketRepository ticketRepository;
    private final QueueTicketHistoryService queueTicketHistoryService;
    
    @Override
    public QueueRating crearRating(CrearRatingRequest request) {
        if (ticketRepository.findById(request.getTicketId()).isEmpty()) {
            throw new RuntimeException("Ticket no encontrado con ID: " + request.getTicketId());
        }

        List<QueueTicketHistory> historial =
                queueTicketHistoryService.obtenerHistorialPorTicket(request.getTicketId());

        Long agentId = historial.stream()
                .filter(h -> "COMPLETED".equals(h.getNewStatus()))
                .findFirst()
                .map(QueueTicketHistory::getAgentId)
                .orElseThrow(() -> new RuntimeException("El ticket no ha sido completado aún"));

        LocalDateTime createdAt = request.getTimestamp() != null
                ? request.getTimestamp()
                : LocalDateTime.now(ZoneId.of("America/Lima"));

        QueueRating saved = queueRatingRepository.save(QueueRating.builder()
                .ticketId(request.getTicketId())
                .agentId(agentId)
                .score(request.getScore())
                .comment(request.getComment())
                .createdAt(createdAt)
                .build());

        log.info("[QueueRating] Rating creado id {} agente {}", saved.getId(), agentId);
        return saved;
    }
}
