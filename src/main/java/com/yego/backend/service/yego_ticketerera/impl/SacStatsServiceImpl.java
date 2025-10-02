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
import java.util.List;
import java.util.Map;
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
        
        // Crear mapa de usuarios para búsqueda eficiente
        Map<Long, User> userMap = sacUsers.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        // Obtener todos los queue agents para mapear agentId -> userId
        List<QueueAgent> queueAgents = queueAgentRepository.findAll();
        Map<Long, Long> agentIdToUserIdMap = queueAgents.stream()
                .collect(Collectors.toMap(
                    QueueAgent::getId,
                    QueueAgent::getUserId
                ));
        log.info("🔄 Queue agents encontrados: {}", queueAgents.size());
        
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
                .map(rating -> convertirARecentRating(rating, userMap, agentIdToUserIdMap))
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
        // Obtener tickets asignados a este SAC
        List<Ticket> sacTickets = ticketRepository.findByAgentId(sacUser.getId());
        
        // Obtener calificaciones de este SAC ordenadas por fecha
        List<QueueRating> sacRatings = queueRatingRepository.findByAgentIdOrderByCreatedAtDesc(sacUser.getId());
        
        // Calcular métricas usando la base de datos directamente
        int totalTickets = (int) ticketRepository.countByAgentId(sacUser.getId());
        int completedTickets = (int) ticketRepository.countCompletedTicketsByAgentId(sacUser.getId());
        
        // Calcular promedio de calificaciones usando la base de datos
        double averageRating = sacRatings.stream()
                .mapToDouble(QueueRating::getScore)
                .average()
                .orElse(0.0);
        
        int totalRatings = (int) queueRatingRepository.countByAgentId(sacUser.getId());
        
        double satisfactionPercentage = totalTickets > 0 ? 
                (double) completedTickets / totalTickets * 100 : 0.0;
        
        // Calcular tiempo promedio de respuesta (simulado)
        String averageResponseTime = calcularTiempoPromedioRespuesta(sacTickets);
        
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
                .completedTickets(completedTickets)
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .totalRatings(totalRatings)
                .satisfactionPercentage(Math.round(satisfactionPercentage * 100.0) / 100.0)
                .averageResponseTime(averageResponseTime)
                .lastActivity(lastActivity)
                .ratings(ratings)
                .build();
    }
    
    private String calcularTiempoPromedioRespuesta(List<Ticket> tickets) {
        if (tickets.isEmpty()) return "0 min";
        
        // Simular cálculo de tiempo promedio (en un caso real, calcularías la diferencia entre created_at y called_at)
        Random random = new Random();
        int minTime = 10;
        int maxTime = 30;
        int averageMinutes = random.nextInt(maxTime - minTime + 1) + minTime;
        
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
    
    private RecentRatingResponse convertirARecentRating(QueueRating rating, Map<Long, User> userMap, Map<Long, Long> agentIdToUserIdMap) {
        // Primero obtener el userId desde queue_agents usando el agentId
        Long userId = agentIdToUserIdMap.get(rating.getAgentId());
        
        String sacName;
        if (userId != null) {
            // Ahora buscar el usuario usando el userId obtenido
            User user = userMap.get(userId);
            if (user != null) {
                sacName = user.getName();
            } else {
                log.warn("⚠️ No se encontró usuario con userId: {} (agentId: {})", userId, rating.getAgentId());
                sacName = "SAC (AgentID: " + rating.getAgentId() + ")";
            }
        } else {
            log.warn("⚠️ No se encontró queue agent con agentId: {}", rating.getAgentId());
            sacName = "SAC (AgentID: " + rating.getAgentId() + ")";
        }
        
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
