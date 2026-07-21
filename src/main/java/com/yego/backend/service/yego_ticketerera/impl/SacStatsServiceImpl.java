package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.RecentRatingResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.SacPerformanceResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.SacPerformanceResponse.RatingResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.HourlyDistribution;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.TicketTraceEventResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.TicketTraceabilityResponse;
import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import com.yego.backend.entity.yego_ticketerera.entities.Option;
import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;
import com.yego.backend.entity.yego_ticketerera.entities.Sede;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.repository.yego_ticketerera.QueueAgentRepository;
import com.yego.backend.repository.yego_ticketerera.QueueRatingRepository;
import com.yego.backend.repository.yego_ticketerera.QueueTicketHistoryRepository;
import com.yego.backend.repository.yego_ticketerera.SedeRepository;
import com.yego.backend.repository.yego_ticketerera.TicketRepository;
import com.yego.backend.repository.yego_ticketerera.ModuloAtencionRepository;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private final SedeRepository sedeRepository;
    private final OptionRepository optionRepository;
    private final ModuloAtencionRepository moduloAtencionRepository;
    private final QueueTicketHistoryRepository queueTicketHistoryRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE_ID = ZoneId.of("America/Lima");
    
    @Override
    @Transactional(readOnly = true)
    public SacStatsResponse obtenerTodasLasEstadisticas(String fechaInicio, String fechaFin, Long sedeId) {
        return calcularEstadisticas(fechaInicio, fechaFin, obtenerUsuariosSac(), sedeId);
    }

    private SacStatsResponse calcularEstadisticas(String fechaInicio, String fechaFin, List<User> sacUsers, Long sedeId) {
        log.debug("Estadísticas SAC fecha inicio {} fin {} sedeId {}", fechaInicio, fechaFin, sedeId);
        long startTime = System.currentTimeMillis();

        // Cargar todas las sedes activas en un mapa
        Map<Long, String> sedeMap = sedeRepository.findByActiveTrueOrderByNameAsc().stream()
                .collect(Collectors.toMap(Sede::getId, Sede::getName));

        // Parsear fechas si se proporcionan
        LocalDateTime fechaInicioDT = null;
        LocalDateTime fechaFinDT = null;
        boolean tieneFiltroFecha = false;

        if (fechaInicio != null && !fechaInicio.isEmpty() && fechaFin != null && !fechaFin.isEmpty()) {
            try {
                LocalDate inicio = LocalDate.parse(fechaInicio, ISO_DATE_FORMATTER);
                LocalDate fin = LocalDate.parse(fechaFin, ISO_DATE_FORMATTER);
                fechaInicioDT = inicio.atStartOfDay().atZone(ZONE_ID).toLocalDateTime();
                fechaFinDT = fin.atTime(LocalTime.MAX).atZone(ZONE_ID).toLocalDateTime();
                tieneFiltroFecha = true;
                log.debug("Filtro fecha {} a {}", fechaInicioDT, fechaFinDT);
            } catch (DateTimeParseException e) {
                log.warn("Error parseando fechas: {}. Se usan todos los datos.", e.getMessage());
            }
        }
        log.debug("Usuarios SAC: {}", sacUsers.size());
        
        // 2. Obtener estadísticas agregadas (consultas optimizadas)
        long totalTickets;
        long totalRatings;
        Double averageRating;
        
        if (tieneFiltroFecha) {
            if (sedeId != null) {
                totalTickets = ticketRepository.countBySedeIdAndCreatedAtBetween(sedeId, fechaInicioDT, fechaFinDT);
                totalRatings = queueRatingRepository.countBySedeIdAndCreatedAtBetween(sedeId, fechaInicioDT, fechaFinDT);
                averageRating = queueRatingRepository.getAverageRatingBySedeIdAndDateRange(sedeId, fechaInicioDT, fechaFinDT);
            } else {
                totalTickets = ticketRepository.countByCreatedAtBetween(fechaInicioDT, fechaFinDT);
                totalRatings = queueRatingRepository.countByCreatedAtBetween(fechaInicioDT, fechaFinDT);
                averageRating = queueRatingRepository.getAverageRatingByDateRange(fechaInicioDT, fechaFinDT);
            }
        } else {
            if (sedeId != null) {
                totalTickets = ticketRepository.countBySedeId(sedeId);
                totalRatings = queueRatingRepository.countBySedeId(sedeId);
                averageRating = queueRatingRepository.getAverageRatingBySedeId(sedeId);
            } else {
                totalTickets = ticketRepository.count();
                totalRatings = queueRatingRepository.count();
                averageRating = queueRatingRepository.getAverageRating();
            }
        }
        
        if (averageRating == null) {
            averageRating = 0.0;
        }
        
        log.debug("Totales tickets {} ratings {} promedio {}", 
            totalTickets, totalRatings, averageRating);
        
        // 4. Obtener IDs de usuarios para consultas batch
        List<Long> userIds = sacUsers.stream().map(User::getId).collect(Collectors.toList());
        
        // 5. Obtener todos los tickets del alcance. La trazabilidad también debe
        // incluir tickets aún no asignados a un operador.
        List<Ticket> reportTickets;
        if (tieneFiltroFecha && sedeId != null) {
            reportTickets = ticketRepository.findBySedeIdAndCreatedAtBetween(sedeId, fechaInicioDT, fechaFinDT);
        } else if (tieneFiltroFecha) {
            reportTickets = ticketRepository.findByCreatedAtBetween(fechaInicioDT, fechaFinDT);
        } else if (sedeId != null) {
            reportTickets = ticketRepository.findBySedeId(sedeId);
        } else {
            reportTickets = ticketRepository.findAll();
        }
        Set<Long> sacUserIds = new HashSet<>(userIds);
        List<Ticket> allSacTickets = reportTickets.stream()
                .filter(ticket -> ticket.getUserId() != null && sacUserIds.contains(ticket.getUserId()))
                .collect(Collectors.toList());
        Map<Long, List<Ticket>> ticketsByUserId = allSacTickets.stream()
                .collect(Collectors.groupingBy(Ticket::getUserId));
        
        // 6. Obtener todos los ticket IDs para buscar calificaciones
        Set<Long> allTicketIds = reportTickets.stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.COMPLETED)
                .map(Ticket::getId)
                .collect(Collectors.toSet());
        
        // 7. Obtener todas las calificaciones en una sola query batch (con o sin filtro de fecha)
        final Map<Long, List<QueueRating>> ratingsByTicketId;
        if (!allTicketIds.isEmpty()) {
            List<QueueRating> allRatings;
            if (tieneFiltroFecha) {
                allRatings = queueRatingRepository.findByTicketIdInAndCreatedAtBetween(
                    new ArrayList<>(allTicketIds), fechaInicioDT, fechaFinDT);
            } else {
                allRatings = queueRatingRepository.findByTicketIdIn(new ArrayList<>(allTicketIds));
            }
            ratingsByTicketId = allRatings.stream()
                    .collect(Collectors.groupingBy(QueueRating::getTicketId));
        } else {
            ratingsByTicketId = new HashMap<>();
        }
        
        // 8. Calcular rendimiento por SAC y por sede real del ticket.
        // Un operador puede tener historial en más de una sede si fue reasignado.
        List<SacPerformanceResponse> sacPerformance = calcularRendimientoPorSede(
                sacUsers, ticketsByUserId, ratingsByTicketId, sedeMap, sedeId);
        
        // 9. Obtener top performers
        List<SacPerformanceResponse> topPerformers = sacPerformance.stream()
                .filter(sac -> sac.getTotalTickets() > 0)
                .sorted((a, b) -> Double.compare(b.getSatisfactionPercentage(), a.getSatisfactionPercentage()))
                .limit(3)
                .collect(Collectors.toList());
        
        // 10. Obtener calificaciones recientes (solo las últimas 10, con o sin filtro de fecha)
        Pageable pageable = PageRequest.of(0, 10);
        List<QueueRating> recentRatings;
        if (sedeId != null && tieneFiltroFecha) {
            recentRatings = queueRatingRepository.findRecentRatingsBySedeIdAndDateRange(
                    pageable, sedeId, fechaInicioDT, fechaFinDT);
        } else if (sedeId != null) {
            recentRatings = queueRatingRepository.findRecentRatingsBySedeId(pageable, sedeId);
        } else if (tieneFiltroFecha) {
            recentRatings = queueRatingRepository.findRecentRatingsByDateRange(pageable, fechaInicioDT, fechaFinDT);
        } else {
            recentRatings = queueRatingRepository.findRecentRatings(pageable);
        }
        List<RecentRatingResponse> recentRatingsResponse = convertirARecentRatingsOptimizado(recentRatings);
        
        long endTime = System.currentTimeMillis();
        log.debug("Estadísticas SAC calculadas en {} ms", (endTime - startTime));

        // Distribución horaria de tickets
        List<HourlyDistribution> hourlyDistribution = calcularDistribucionHoraria(reportTickets);

        // Distribución horaria por sede
        Map<Long, List<Ticket>> ticketsBySede = reportTickets.stream()
                .filter(t -> t.getSedeId() != null)
                .collect(Collectors.groupingBy(Ticket::getSedeId));
        List<SacStatsResponse.HourlyBySede> hourlyBySede = ticketsBySede.entrySet().stream()
                .map(entry -> SacStatsResponse.HourlyBySede.builder()
                        .sedeId(entry.getKey())
                        .sedeName(sedeMap.getOrDefault(entry.getKey(), "Sede " + entry.getKey()))
                        .hourlyDistribution(calcularDistribucionHoraria(entry.getValue()))
                        .build())
                .sorted(Comparator.comparing(SacStatsResponse.HourlyBySede::getSedeName))
                .collect(Collectors.toList());
        
        int totalSacsReportados = sedeId != null
                ? (int) sacPerformance.stream().filter(sac -> sac.getTotalTickets() > 0).count()
                : sacUsers.size();

        int openTickets = (int) reportTickets.stream()
                .filter(ticket -> ticket.getStatus() == Ticket.TicketStatus.WAITING
                        || ticket.getStatus() == Ticket.TicketStatus.CALLED
                        || ticket.getStatus() == Ticket.TicketStatus.IN_PROGRESS)
                .count();
        int completedTickets = (int) reportTickets.stream()
                .filter(ticket -> ticket.getStatus() == Ticket.TicketStatus.COMPLETED)
                .count();
        int cancelledTickets = (int) reportTickets.stream()
                .filter(ticket -> ticket.getStatus() == Ticket.TicketStatus.CANCELLED)
                .count();
        List<TicketTraceabilityResponse> ticketTraceability = construirTrazabilidad(
                reportTickets, sedeMap, ratingsByTicketId);

        return SacStatsResponse.builder()
                .totalSACs(totalSacsReportados)
                .totalTickets((int) totalTickets)
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .totalRatings((int) totalRatings)
                .openTickets(openTickets)
                .completedTickets(completedTickets)
                .cancelledTickets(cancelledTickets)
                .traceabilityTotal(reportTickets.size())
                .sacPerformance(sacPerformance)
                .topPerformers(topPerformers)
                .recentRatings(recentRatingsResponse)
                .hourlyDistribution(hourlyDistribution)
                .hourlyBySede(hourlyBySede)
                .ticketTraceability(ticketTraceability)
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
    
    private SacPerformanceResponse calcularRendimientoSacOptimizado(
            User sacUser,
            List<Ticket> sacTickets,
            Map<Long, List<QueueRating>> ratingsByTicketId,
            Map<Long, String> sedeMap,
            Long performanceSedeId) {
        
        Long userId = sacUser.getId();
        
        Long sedeId = performanceSedeId;
        String sedeName = (sedeId != null && sedeMap.containsKey(sedeId))
                ? sedeMap.get(sedeId)
                : "Sin sede";
        
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
                .sedeId(sedeId)
                .sedeName(sedeName)
                .totalTickets(totalTickets)
                .completedTickets(completedTicketsCount)
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .totalRatings(totalRatings)
                .satisfactionPercentage(Math.round(satisfactionPercentage * 100.0) / 100.0)
                .averageResponseTime(averageResponseTime)
                .ratings(ratings)
                .build();
    }

    private List<SacPerformanceResponse> calcularRendimientoPorSede(
            List<User> sacUsers,
            Map<Long, List<Ticket>> ticketsByUserId,
            Map<Long, List<QueueRating>> ratingsByTicketId,
            Map<Long, String> sedeMap,
            Long sedeIdFiltro) {
        List<SacPerformanceResponse> performance = new ArrayList<>();

        for (User user : sacUsers) {
            List<Ticket> userTickets = ticketsByUserId.getOrDefault(user.getId(), Collections.emptyList());

            if (sedeIdFiltro != null) {
                if (!userTickets.isEmpty() || sedeIdFiltro.equals(user.getSedeId())) {
                    performance.add(calcularRendimientoSacOptimizado(
                            user, userTickets, ratingsByTicketId, sedeMap, sedeIdFiltro));
                }
                continue;
            }

            Map<Long, List<Ticket>> ticketsBySede = userTickets.stream()
                    .filter(ticket -> ticket.getSedeId() != null)
                    .collect(Collectors.groupingBy(Ticket::getSedeId));

            if (ticketsBySede.isEmpty()) {
                performance.add(calcularRendimientoSacOptimizado(
                        user, Collections.emptyList(), ratingsByTicketId, sedeMap, user.getSedeId()));
                continue;
            }

            ticketsBySede.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> performance.add(calcularRendimientoSacOptimizado(
                            user, entry.getValue(), ratingsByTicketId, sedeMap, entry.getKey())));
        }

        return performance;
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

    private List<TicketTraceabilityResponse> construirTrazabilidad(
            List<Ticket> reportTickets,
            Map<Long, String> sedeMap,
            Map<Long, List<QueueRating>> ratingsByTicketId) {
        if (reportTickets.isEmpty()) return Collections.emptyList();

        List<Ticket> visibleTickets = reportTickets.stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(200)
                .collect(Collectors.toList());
        Set<Long> ticketIds = visibleTickets.stream().map(Ticket::getId).collect(Collectors.toSet());

        Set<Long> optionIds = visibleTickets.stream()
                .map(Ticket::getOptionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Option> options = optionRepository.findAllById(optionIds).stream()
                .collect(Collectors.toMap(Option::getId, option -> option));
        Set<Long> parentOptionIds = options.values().stream()
                .map(Option::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        optionRepository.findAllById(parentOptionIds)
                .forEach(option -> options.putIfAbsent(option.getId(), option));

        Set<Long> moduleIds = visibleTickets.stream()
                .map(Ticket::getModuleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> modules = moduloAtencionRepository.findAllById(moduleIds).stream()
                .collect(Collectors.toMap(ModuloAtencion::getId, ModuloAtencion::getName));

        Set<Long> operatorIds = visibleTickets.stream()
                .map(Ticket::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> operators = userRepository.findAllById(operatorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        Map<Long, List<QueueTicketHistory>> historyByTicket = ticketIds.isEmpty()
                ? Collections.emptyMap()
                : queueTicketHistoryRepository.findByTicketIdInOrderByCreatedAtAsc(ticketIds).stream()
                        .collect(Collectors.groupingBy(QueueTicketHistory::getTicketId));

        return visibleTickets.stream()
                .map(ticket -> {
                    Option selectedOption = options.get(ticket.getOptionId());
                    Option parentOption = selectedOption != null && selectedOption.getParentId() != null
                            ? options.get(selectedOption.getParentId())
                            : null;
                    List<QueueRating> ticketRatings = ratingsByTicketId.getOrDefault(
                            ticket.getId(), Collections.emptyList());
                    QueueRating latestRating = ticketRatings.stream()
                            .max(Comparator.comparing(QueueRating::getCreatedAt))
                            .orElse(null);

                    List<TicketTraceEventResponse> events = new ArrayList<>();
                    events.add(TicketTraceEventResponse.builder()
                            .status("GENERATED")
                            .label("Ticket generado")
                            .occurredAt(ticket.getCreatedAt())
                            .build());
                    historyByTicket.getOrDefault(ticket.getId(), Collections.emptyList()).stream()
                            .map(history -> TicketTraceEventResponse.builder()
                                    .status(history.getNewStatus())
                                    .label(etiquetaEstado(history.getNewStatus()))
                                    .occurredAt(history.getCreatedAt())
                                    .notes(history.getNotes())
                                    .build())
                            .forEach(events::add);
                    if (latestRating != null) {
                        events.add(TicketTraceEventResponse.builder()
                                .status("RATED")
                                .label("Calificación " + latestRating.getScore() + "/5")
                                .occurredAt(latestRating.getCreatedAt())
                                .notes(latestRating.getComment())
                                .build());
                    }
                    events.sort(Comparator.comparing(TicketTraceEventResponse::getOccurredAt,
                            Comparator.nullsLast(Comparator.naturalOrder())));

                    return TicketTraceabilityResponse.builder()
                            .id(ticket.getId())
                            .ticketNumber(ticket.getTicketNumber())
                            .status(ticket.getStatus().name())
                            .sedeId(ticket.getSedeId())
                            .sedeName(sedeMap.getOrDefault(ticket.getSedeId(), "Sin sede"))
                            .optionId(ticket.getOptionId())
                            .categoryName(parentOption != null ? parentOption.getName() : null)
                            .optionName(selectedOption != null
                                    ? selectedOption.getName()
                                    : "Opción #" + ticket.getOptionId())
                            .licenseNumber(ticket.getLicenseNumber())
                            .moduleId(ticket.getModuleId())
                            .moduleName(modules.get(ticket.getModuleId()))
                            .operatorId(ticket.getUserId())
                            .operatorName(operators.get(ticket.getUserId()))
                            .createdAt(ticket.getCreatedAt())
                            .calledAt(ticket.getCalledAt())
                            .completedAt(ticket.getCompletedAt())
                            .rating(latestRating != null ? latestRating.getScore() : null)
                            .events(events)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String etiquetaEstado(String status) {
        if (status == null) return "Estado actualizado";
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "WAITING" -> "En espera";
            case "CALLED" -> "Ticket llamado";
            case "IN_PROGRESS" -> "Atención iniciada";
            case "COMPLETED" -> "Atención completada";
            case "CANCELLED" -> "Ticket cancelado";
            default -> "Estado actualizado";
        };
    }

    private List<HourlyDistribution> calcularDistribucionHoraria(List<Ticket> tickets) {
        long[] counts = new long[24];
        for (Ticket ticket : tickets) {
            if (ticket.getCreatedAt() != null) {
                int hour = ticket.getCreatedAt().atZone(ZONE_ID).getHour();
                counts[hour]++;
            }
        }

        String[] labels = {"12am","01am","02am","03am","04am","05am","06am","07am","08am","09am",
                           "10am","11am","12pm","01pm","02pm","03pm","04pm","05pm","06pm","07pm",
                           "08pm","09pm","10pm","11pm"};

        List<HourlyDistribution> result = new java.util.ArrayList<>();
        for (int h = 0; h < 24; h++) {
            result.add(HourlyDistribution.builder()
                    .hour(h)
                    .label(labels[h])
                    .count(counts[h])
                    .build());
        }
        return result;
    }
}
