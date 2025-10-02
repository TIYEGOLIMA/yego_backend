package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.RecentRatingResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.SacPerformanceResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.SacPerformanceResponse.RatingResponse;
import com.yego.backend.entity.yego_ticketerera.entities.QueueAgent;
import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.repository.yego_ticketerera.QueueAgentRepository;
import com.yego.backend.repository.yego_ticketerera.QueueRatingRepository;
import com.yego.backend.repository.yego_ticketerera.TicketRepository;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SacStatsServiceImpl implements SacStatsService {
    
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final QueueRatingRepository queueRatingRepository;
    private final QueueAgentRepository queueAgentRepository;
    
    @Override
    @Transactional(readOnly = true)
    public SacStatsResponse obtenerTodasLasEstadisticas() {
        log.info("📊 Calculando estadísticas generales de SAC");
        
        // Obtener todos los usuarios con rol SAC
        List<User> sacUsers = userRepository.findByRole("SAC");
        log.info("👥 Usuarios SAC encontrados: {}", sacUsers.size());
        
        // Obtener todos los tickets
        List<Ticket> allTickets = ticketRepository.findAll();
        
        // Obtener todas las calificaciones ordenadas por fecha
        List<QueueRating> allRatings = queueRatingRepository.findAllByOrderByCreatedAtDesc();
        log.info("⭐ Calificaciones encontradas: {}", allRatings.size());
        
        // Calcular estadísticas generales
        int totalSACs = sacUsers.size();
        int totalTickets = allTickets.size();
        double averageRating = allRatings.stream()
                .mapToDouble(QueueRating::getScore)
                .average()
                .orElse(0.0);
        int totalRatings = allRatings.size();
        
        // Calcular rendimiento de cada SAC
        List<SacPerformanceResponse> sacPerformance = sacUsers.stream()
                .map(this::calcularRendimientoSac)
                .collect(Collectors.toList());
        
        // Obtener top performers (ordenados por satisfacción)
        List<SacPerformanceResponse> topPerformers = sacPerformance.stream()
                .sorted((a, b) -> Double.compare(b.getSatisfactionPercentage(), a.getSatisfactionPercentage()))
                .limit(3)
                .collect(Collectors.toList());
        
        // Obtener calificaciones recientes (ya están ordenadas por fecha en la consulta)
        List<RecentRatingResponse> recentRatings = allRatings.stream()
                .limit(10)
                .map(this::convertirARecentRating)
                .collect(Collectors.toList());
        
        return SacStatsResponse.builder()
                .totalSACs(totalSACs)
                .totalTickets(totalTickets)
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .totalRatings(totalRatings)
                .sacPerformance(sacPerformance)
                .topPerformers(topPerformers)
                .recentRatings(recentRatings)
                .build();
    }
    
    
    private SacPerformanceResponse calcularRendimientoSac(User sacUser) {
        Long userId = sacUser.getId();
        log.info("🔍 Calculando métricas para SAC: {} (userId: {})", sacUser.getName(), userId);
        
        // Obtener tickets del usuario usando userId
        List<Ticket> sacTickets = ticketRepository.findByUserId(userId);
        log.info("📊 Tickets encontrados para {}: {}", sacUser.getName(), sacTickets.size());
        
        // Obtener tickets completados (COMPLETED) del usuario
        List<Ticket> completedTickets = sacTickets.stream()
                .filter(ticket -> ticket.getStatus() == Ticket.TicketStatus.COMPLETED)
                .collect(Collectors.toList());
        
        // Obtener tickets cancelados (CANCELLED) del usuario
        List<Ticket> cancelledTickets = sacTickets.stream()
                .filter(ticket -> ticket.getStatus() == Ticket.TicketStatus.CANCELLED)
                .collect(Collectors.toList());
        
        int completedTicketsCount = completedTickets.size();
        int cancelledTicketsCount = cancelledTickets.size();
        int totalTickets = completedTicketsCount + cancelledTicketsCount;
        
        // Obtener calificaciones de los tickets completados
        List<QueueRating> sacRatings = new ArrayList<>();
        for (Ticket completedTicket : completedTickets) {
            List<QueueRating> ticketRatings = queueRatingRepository.findByTicketId(completedTicket.getId());
            sacRatings.addAll(ticketRatings);
        }
        
        log.info("⭐ Calificaciones encontradas para {}: {}", sacUser.getName(), sacRatings.size());
        
        // Calcular promedio de calificaciones
        double averageRating = sacRatings.stream()
                .mapToDouble(QueueRating::getScore)
                .average()
                .orElse(0.0);
        
        int totalRatings = sacRatings.size();
        
        double satisfactionPercentage = totalTickets > 0 ? 
                (double) completedTicketsCount / totalTickets * 100 : 0.0;
        
        // Calcular tiempo promedio de respuesta real basado en calledAt y completedAt
        String averageResponseTime = calcularTiempoPromedioRespuestaReal(completedTickets);
        
        // Obtener última actividad
        String lastActivity = sacUser.getLastLogin() != null ? 
                sacUser.getLastLogin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : 
                "Nunca";
        
        // Convertir ratings a formato de respuesta
        List<RatingResponse> ratings = sacRatings.stream()
                .limit(3) // Solo los últimos 3 ratings
                .map(this::convertirARatingResponse)
                .collect(Collectors.toList());
        
        return SacPerformanceResponse.builder()
                .id(sacUser.getId())
                .name(sacUser.getName())
                .username(sacUser.getUsername())
                .totalTickets(totalTickets)
                .completedTickets(completedTicketsCount)
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .totalRatings(totalRatings)
                .satisfactionPercentage(Math.round(satisfactionPercentage * 100.0) / 100.0)
                .averageResponseTime(averageResponseTime)
                .lastActivity(lastActivity)
                .ratings(ratings)
                .build();
    }
    
    private String calcularTiempoPromedioRespuestaReal(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return "0 min";
        }
        
        // Filtrar tickets que tienen calledAt y completedAt
        List<Ticket> completedTickets = tickets.stream()
                .filter(ticket -> ticket.getCalledAt() != null && ticket.getCompletedAt() != null)
                .collect(Collectors.toList());
        
        if (completedTickets.isEmpty()) {
            return "0 min";
        }
        
        // Calcular tiempo promedio en minutos
        long totalMinutes = completedTickets.stream()
                .mapToLong(ticket -> {
                    java.time.Duration duration = java.time.Duration.between(
                            ticket.getCalledAt(), 
                            ticket.getCompletedAt()
                    );
                    return duration.toMinutes();
                })
                .sum();
        
        long averageMinutes = totalMinutes / completedTickets.size();
        
        return averageMinutes + " min";
    }
    
    private RatingResponse convertirARatingResponse(QueueRating rating) {
        // Obtener el número real del ticket
        String ticketNumber = ticketRepository.findById(rating.getTicketId())
                .map(Ticket::getTicketNumber)
                .orElse("TKT-" + rating.getTicketId());
        
        return RatingResponse.builder()
                .id(rating.getId())
                .score(rating.getScore())
                .comment(rating.getComment())
                .ticketNumber(ticketNumber)
                .date(rating.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .build();
    }
    
    private RecentRatingResponse convertirARecentRating(QueueRating rating) {
        // Buscar el usuario usando el agentId del rating
        String sacName = queueAgentRepository.findById(rating.getAgentId())
                .map(queueAgent -> {
                    Long userId = queueAgent.getUserId();
                    return userRepository.findById(userId)
                            .map(User::getName)
                            .orElse("Usuario ID: " + userId);
                })
                .orElse("Agent ID: " + rating.getAgentId());
        
        // Obtener el número real del ticket
        String ticketNumber = ticketRepository.findById(rating.getTicketId())
                .map(Ticket::getTicketNumber)
                .orElse("TKT-" + rating.getTicketId());
        
        return RecentRatingResponse.builder()
                .id(rating.getId())
                .sacName(sacName)
                .score(rating.getScore())
                .comment(rating.getComment())
                .ticketNumber(ticketNumber)
                .date(rating.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .build();
    }
}
