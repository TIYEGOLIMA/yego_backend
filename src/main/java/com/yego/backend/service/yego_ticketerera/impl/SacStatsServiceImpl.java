package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.RecentRatingResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.SacPerformanceResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.SacPerformanceResponse.RatingResponse;
import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.repository.yego_ticketerera.QueueAgentRepository;
import com.yego.backend.repository.yego_ticketerera.QueueRatingRepository;
import com.yego.backend.repository.yego_ticketerera.TicketRepository;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SacStatsServiceImpl implements SacStatsService {
    
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final QueueRatingRepository queueRatingRepository;
    private final QueueAgentRepository queueAgentRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    @Override
    @Transactional(readOnly = true)
    public SacStatsResponse obtenerTodasLasEstadisticas() {
        log.info("📊 Calculando estadísticas generales de SAC (optimizado)");
        long startTime = System.currentTimeMillis();
        
        // 1. Obtener usuarios SAC (una sola query)
        List<User> sacUsers = obtenerUsuariosSac();
        log.info("👥 Usuarios SAC encontrados: {}", sacUsers.size());
        
        if (sacUsers.isEmpty()) {
            return construirRespuestaVacia();
        }
        
        // 2. Obtener estadísticas agregadas (consultas optimizadas)
        long totalTickets = ticketRepository.count();
        long totalRatings = queueRatingRepository.count();
        
        // 3. Calcular promedio de calificaciones (una sola query agregada SQL)
        Double averageRating = queueRatingRepository.getAverageRating();
        if (averageRating == null) {
            averageRating = 0.0;
        }
        
        log.info("📊 Estadísticas generales - Tickets: {}, Ratings: {}, Promedio: {}", 
            totalTickets, totalRatings, averageRating);
        
        // 4. Obtener IDs de usuarios para consultas batch
        List<Long> userIds = sacUsers.stream().map(User::getId).collect(Collectors.toList());
        
        // 5. Obtener todos los tickets de usuarios SAC en una sola query
        List<Ticket> allSacTickets = ticketRepository.findByUserIdIn(userIds);
        Map<Long, List<Ticket>> ticketsByUserId = allSacTickets.stream()
                .collect(Collectors.groupingBy(Ticket::getUserId));
        
        // 6. Obtener todos los ticket IDs para buscar calificaciones
        Set<Long> allTicketIds = allSacTickets.stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.COMPLETED)
                .map(Ticket::getId)
                .collect(Collectors.toSet());
        
        // 7. Obtener todas las calificaciones en una sola query batch
        final Map<Long, List<QueueRating>> ratingsByTicketId;
        if (!allTicketIds.isEmpty()) {
            List<QueueRating> allRatings = queueRatingRepository.findByTicketIdIn(new ArrayList<>(allTicketIds));
            ratingsByTicketId = allRatings.stream()
                    .collect(Collectors.groupingBy(QueueRating::getTicketId));
        } else {
            ratingsByTicketId = new HashMap<>();
        }
        
        // 8. Calcular rendimiento de cada SAC (sin queries adicionales)
        List<SacPerformanceResponse> sacPerformance = sacUsers.stream()
                .map(user -> calcularRendimientoSacOptimizado(
                    user, 
                    ticketsByUserId.getOrDefault(user.getId(), Collections.emptyList()),
                    ratingsByTicketId
                ))
                .collect(Collectors.toList());
        
        // 9. Obtener top performers
        List<SacPerformanceResponse> topPerformers = sacPerformance.stream()
                .filter(sac -> sac.getTotalTickets() > 0)
                .sorted((a, b) -> Double.compare(b.getSatisfactionPercentage(), a.getSatisfactionPercentage()))
                .limit(3)
                .collect(Collectors.toList());
        
        // 10. Obtener calificaciones recientes (solo las últimas 10)
        Pageable pageable = PageRequest.of(0, 10);
        List<QueueRating> recentRatings = queueRatingRepository.findRecentRatings(pageable);
        List<RecentRatingResponse> recentRatingsResponse = convertirARecentRatingsOptimizado(recentRatings);
        
        long endTime = System.currentTimeMillis();
        log.info("✅ Estadísticas calculadas en {}ms", (endTime - startTime));
        
        return SacStatsResponse.builder()
                .totalSACs(sacUsers.size())
                .totalTickets((int) totalTickets)
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .totalRatings((int) totalRatings)
                .sacPerformance(sacPerformance)
                .topPerformers(topPerformers)
                .recentRatings(recentRatingsResponse)
                .build();
    }
    
    private List<User> obtenerUsuariosSac() {
        List<User> sacUsers = userRepository.findByRoleName("SAC");
        if (sacUsers.isEmpty()) {
            sacUsers = userRepository.findByRoleName("sac");
        }
        if (sacUsers.isEmpty()) {
            sacUsers = userRepository.findByRoleName("Sac");
        }
        return sacUsers;
    }
    
    private SacStatsResponse construirRespuestaVacia() {
        return SacStatsResponse.builder()
                .totalSACs(0)
                .totalTickets(0)
                .averageRating(0.0)
                .totalRatings(0)
                .sacPerformance(Collections.emptyList())
                .topPerformers(Collections.emptyList())
                .recentRatings(Collections.emptyList())
                .build();
    }
    
    private SacPerformanceResponse calcularRendimientoSacOptimizado(
            User sacUser,
            List<Ticket> sacTickets,
            Map<Long, List<QueueRating>> ratingsByTicketId) {
        
        Long userId = sacUser.getId();
        
        // Filtrar tickets completados y cancelados en memoria
        List<Ticket> completedTickets = sacTickets.stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.COMPLETED)
                .collect(Collectors.toList());
        
        List<Ticket> cancelledTickets = sacTickets.stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.CANCELLED)
                .collect(Collectors.toList());
        
        int completedTicketsCount = completedTickets.size();
        int cancelledTicketsCount = cancelledTickets.size();
        int totalTickets = completedTicketsCount + cancelledTicketsCount;
        
        // Obtener calificaciones de tickets completados (ya están en memoria)
        List<QueueRating> sacRatings = completedTickets.stream()
                .map(Ticket::getId)
                .flatMap(ticketId -> ratingsByTicketId.getOrDefault(ticketId, Collections.emptyList()).stream())
                .collect(Collectors.toList());
        
        // Calcular métricas
        double averageRating = sacRatings.stream()
                .mapToDouble(QueueRating::getScore)
                .average()
                .orElse(0.0);
        
        int totalRatings = sacRatings.size();
        double satisfactionPercentage = totalTickets > 0 ? 
                (double) completedTicketsCount / totalTickets * 100 : 0.0;
        
        String averageResponseTime = calcularTiempoPromedioRespuesta(completedTickets);
        String lastActivity = sacUser.getLastLogin() != null ? 
                sacUser.getLastLogin().format(DATETIME_FORMATTER) : "Nunca";
        
        // Convertir ratings (solo últimos 3)
        List<RatingResponse> ratings = sacRatings.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(3)
                .map(rating -> convertirARatingResponse(rating, completedTickets))
                .collect(Collectors.toList());
        
        return SacPerformanceResponse.builder()
                .id(userId)
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
    
    private String calcularTiempoPromedioRespuesta(List<Ticket> tickets) {
        List<Ticket> validTickets = tickets.stream()
                .filter(t -> t.getCalledAt() != null && t.getCompletedAt() != null)
                .collect(Collectors.toList());
        
        if (validTickets.isEmpty()) {
            return "0 min";
        }
        
        long totalMinutes = validTickets.stream()
                .mapToLong(t -> Duration.between(t.getCalledAt(), t.getCompletedAt()).toMinutes())
                .sum();
        
        long averageMinutes = totalMinutes / validTickets.size();
        return averageMinutes + " min";
    }
    
    private RatingResponse convertirARatingResponse(QueueRating rating, List<Ticket> tickets) {
        String ticketNumber = tickets.stream()
                .filter(t -> t.getId().equals(rating.getTicketId()))
                .findFirst()
                .map(Ticket::getTicketNumber)
                .orElse("TKT-" + rating.getTicketId());
        
        return RatingResponse.builder()
                .id(rating.getId())
                .score(rating.getScore())
                .comment(rating.getComment())
                .ticketNumber(ticketNumber)
                .date(rating.getCreatedAt().format(DATE_FORMATTER))
                .build();
    }
    
    private List<RecentRatingResponse> convertirARecentRatingsOptimizado(List<QueueRating> ratings) {
        if (ratings.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Obtener todos los ticket IDs y agent IDs en una sola pasada
        Set<Long> ticketIds = ratings.stream()
                .map(QueueRating::getTicketId)
                .collect(Collectors.toSet());
        
        Set<Long> agentIds = ratings.stream()
                .map(QueueRating::getAgentId)
                .collect(Collectors.toSet());
        
        // Obtener todos los tickets en una query batch
        Map<Long, Ticket> ticketsMap = ticketRepository.findAllById(ticketIds).stream()
                .collect(Collectors.toMap(Ticket::getId, t -> t));
        
        // Obtener todos los agentes y usuarios en queries batch
        Map<Long, String> sacNamesMap = new HashMap<>();
        queueAgentRepository.findAllById(agentIds).forEach(agent -> {
            if (agent.getUserId() != null) {
                userRepository.findById(agent.getUserId())
                        .ifPresent(user -> sacNamesMap.put(agent.getId(), user.getName()));
            }
        });
        
        // Construir respuesta usando los maps
        return ratings.stream()
                .map(rating -> {
                    Ticket ticket = ticketsMap.get(rating.getTicketId());
                    String ticketNumber = ticket != null ? ticket.getTicketNumber() : "TKT-" + rating.getTicketId();
                    String sacName = sacNamesMap.getOrDefault(rating.getAgentId(), "Agent ID: " + rating.getAgentId());
                    
                    return RecentRatingResponse.builder()
                            .id(rating.getId())
                            .sacName(sacName)
                            .score(rating.getScore())
                            .comment(rating.getComment())
                            .ticketNumber(ticketNumber)
                            .date(rating.getCreatedAt().format(DATE_FORMATTER))
                            .build();
                })
                .collect(Collectors.toList());
    }
}
