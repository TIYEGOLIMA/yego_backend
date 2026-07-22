package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.SacPerformanceResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.HourlyDistribution;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.OptionSelectionBySedeResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.OptionSelectionResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.TicketTraceEventResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse.TicketTraceabilityResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketTraceabilityPageResponse;
import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import com.yego.backend.entity.yego_ticketerera.entities.Option;
import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;
import com.yego.backend.entity.yego_ticketerera.entities.Sede;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.repository.yego_ticketerera.QueueRatingRepository;
import com.yego.backend.repository.yego_ticketerera.QueueTicketHistoryRepository;
import com.yego.backend.repository.yego_ticketerera.SedeRepository;
import com.yego.backend.repository.yego_ticketerera.TicketRepository;
import com.yego.backend.repository.yego_ticketerera.ModuloAtencionRepository;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import com.yego.backend.service.yego_ticketerera.SacStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final SedeRepository sedeRepository;
    private final OptionRepository optionRepository;
    private final ModuloAtencionRepository moduloAtencionRepository;
    private final QueueTicketHistoryRepository queueTicketHistoryRepository;
    
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE_ID = ZoneId.of("America/Lima");

    private record DateRange(LocalDateTime start, LocalDateTime end) {}
    
    @Override
    @Transactional(readOnly = true)
    public SacStatsResponse obtenerTodasLasEstadisticas(String fechaInicio, String fechaFin, Long sedeId) {
        return construirReporte(fechaInicio, fechaFin, obtenerUsuariosSac(), sedeId);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketTraceabilityPageResponse obtenerTrazabilidad(
            String fechaInicio,
            String fechaFin,
            Long sedeId,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        DateRange dateRange = parseDateRange(fechaInicio, fechaFin).orElse(null);

        Page<Ticket> ticketPage;
        if (dateRange != null && sedeId != null) {
            ticketPage = ticketRepository.findBySedeIdAndCreatedAtBetween(
                    sedeId, dateRange.start(), dateRange.end(), pageable);
        } else if (dateRange != null) {
            ticketPage = ticketRepository.findByCreatedAtBetween(
                    dateRange.start(), dateRange.end(), pageable);
        } else if (sedeId != null) {
            ticketPage = ticketRepository.findBySedeId(sedeId, pageable);
        } else {
            ticketPage = ticketRepository.findAll(pageable);
        }

        List<Ticket> tickets = ticketPage.getContent();
        Map<Long, List<QueueRating>> ratingsByTicket = cargarRatingsPorTicket(tickets, dateRange);
        Map<Long, String> sedeMap = sedeRepository.findByActiveTrueOrderByNameAsc().stream()
                .collect(Collectors.toMap(Sede::getId, Sede::getName));
        Map<Long, Option> optionCatalog = cargarCatalogoOpciones(tickets);
        List<TicketTraceabilityResponse> content = construirTrazabilidad(
                tickets, sedeMap, ratingsByTicket, optionCatalog);

        return TicketTraceabilityPageResponse.builder()
                .content(content)
                .page(ticketPage.getNumber())
                .size(ticketPage.getSize())
                .totalElements(ticketPage.getTotalElements())
                .totalPages(ticketPage.getTotalPages())
                .first(ticketPage.isFirst())
                .last(ticketPage.isLast())
                .build();
    }

    private Optional<DateRange> parseDateRange(String fechaInicio, String fechaFin) {
        if (fechaInicio == null || fechaInicio.isBlank() || fechaFin == null || fechaFin.isBlank()) {
            return Optional.empty();
        }
        try {
            LocalDate inicio = LocalDate.parse(fechaInicio, ISO_DATE_FORMATTER);
            LocalDate fin = LocalDate.parse(fechaFin, ISO_DATE_FORMATTER);
            return Optional.of(new DateRange(
                    inicio.atStartOfDay().atZone(ZONE_ID).toLocalDateTime(),
                    fin.atTime(LocalTime.MAX).atZone(ZONE_ID).toLocalDateTime()));
        } catch (DateTimeParseException exception) {
            log.warn("Rango de fechas inválido: {} - {}", fechaInicio, fechaFin);
            return Optional.empty();
        }
    }

    private Map<Long, List<QueueRating>> cargarRatingsPorTicket(
            List<Ticket> tickets,
            DateRange dateRange) {
        List<Long> completedTicketIds = tickets.stream()
                .filter(ticket -> ticket.getStatus() == Ticket.TicketStatus.COMPLETED)
                .map(Ticket::getId)
                .collect(Collectors.toList());
        if (completedTicketIds.isEmpty()) return Collections.emptyMap();

        List<QueueRating> ratings = dateRange != null
                ? queueRatingRepository.findByTicketIdInAndCreatedAtBetween(
                        completedTicketIds, dateRange.start(), dateRange.end())
                : queueRatingRepository.findByTicketIdIn(completedTicketIds);
        return ratings.stream().collect(Collectors.groupingBy(QueueRating::getTicketId));
    }

    private SacStatsResponse construirReporte(String fechaInicio, String fechaFin, List<User> sacUsers, Long sedeId) {
        log.debug("Reporte Ticketera fecha inicio {} fin {} sedeId {}", fechaInicio, fechaFin, sedeId);
        long startTime = System.currentTimeMillis();

        Map<Long, String> sedeMap = sedeRepository.findByActiveTrueOrderByNameAsc().stream()
                .collect(Collectors.toMap(Sede::getId, Sede::getName));

        DateRange dateRange = parseDateRange(fechaInicio, fechaFin).orElse(null);
        LocalDateTime fechaInicioDT = dateRange != null ? dateRange.start() : null;
        LocalDateTime fechaFinDT = dateRange != null ? dateRange.end() : null;
        boolean tieneFiltroFecha = dateRange != null;
        log.debug("Usuarios SAC: {}", sacUsers.size());
        
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
        
        List<Long> userIds = sacUsers.stream().map(User::getId).collect(Collectors.toList());
        
        // Incluye tickets sin operador porque también forman parte de la demanda y trazabilidad.
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
        
        Set<Long> allTicketIds = reportTickets.stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.COMPLETED)
                .map(Ticket::getId)
                .collect(Collectors.toSet());
        
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
        
        // Un operador puede tener historial en más de una sede si fue reasignado.
        List<SacPerformanceResponse> sacPerformance = calcularRendimientoPorSede(
                sacUsers, ticketsByUserId, ratingsByTicketId, sedeMap, sedeId);
        
        List<HourlyDistribution> hourlyDistribution = calcularDistribucionHoraria(reportTickets);

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
        Map<Long, Option> optionCatalog = cargarCatalogoOpciones(reportTickets);
        List<OptionSelectionBySedeResponse> optionSelectionsBySede = construirOpcionesPorSede(
                reportTickets, sedeMap, optionCatalog);
        List<Ticket> recentTraceTickets = reportTickets.stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(200)
                .collect(Collectors.toList());
        List<TicketTraceabilityResponse> ticketTraceability = construirTrazabilidad(
                recentTraceTickets, sedeMap, ratingsByTicketId, optionCatalog);

        log.debug("Reporte Ticketera calculado en {} ms", System.currentTimeMillis() - startTime);

        return SacStatsResponse.builder()
                .totalTickets((int) totalTickets)
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .totalRatings((int) totalRatings)
                .openTickets(openTickets)
                .completedTickets(completedTickets)
                .cancelledTickets(cancelledTickets)
                .traceabilityTotal(reportTickets.size())
                .sacPerformance(sacPerformance)
                .hourlyDistribution(hourlyDistribution)
                .hourlyBySede(hourlyBySede)
                .optionSelectionsBySede(optionSelectionsBySede)
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
    
    private SacPerformanceResponse calcularRendimientoSac(
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
        double resolutionPercentage = totalTickets > 0 ?
                (double) completedTicketsCount / totalTickets * 100 : 0.0;
        String averageServiceTime = calcularTiempoPromedioAtencion(completedTickets);
        
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
                .resolutionPercentage(Math.round(resolutionPercentage * 100.0) / 100.0)
                .averageServiceTime(averageServiceTime)
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
                    performance.add(calcularRendimientoSac(
                            user, userTickets, ratingsByTicketId, sedeMap, sedeIdFiltro));
                }
                continue;
            }

            Map<Long, List<Ticket>> ticketsBySede = userTickets.stream()
                    .filter(ticket -> ticket.getSedeId() != null)
                    .collect(Collectors.groupingBy(Ticket::getSedeId));

            if (ticketsBySede.isEmpty()) {
                performance.add(calcularRendimientoSac(
                        user, Collections.emptyList(), ratingsByTicketId, sedeMap, user.getSedeId()));
                continue;
            }

            ticketsBySede.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> performance.add(calcularRendimientoSac(
                            user, entry.getValue(), ratingsByTicketId, sedeMap, entry.getKey())));
        }

        return performance;
    }
    
    private String calcularTiempoPromedioAtencion(List<Ticket> tickets) {
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
    
    private Map<Long, Option> cargarCatalogoOpciones(List<Ticket> tickets) {
        Set<Long> selectedOptionIds = tickets.stream()
                .map(Ticket::getOptionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (selectedOptionIds.isEmpty()) return new HashMap<>();

        Map<Long, Option> options = optionRepository.findAllById(selectedOptionIds).stream()
                .collect(Collectors.toMap(Option::getId, option -> option, (first, ignored) -> first, HashMap::new));
        Set<Long> parentIds = options.values().stream()
                .map(Option::getParentId)
                .filter(Objects::nonNull)
                .filter(parentId -> !options.containsKey(parentId))
                .collect(Collectors.toSet());
        if (!parentIds.isEmpty()) {
            optionRepository.findAllById(parentIds)
                    .forEach(option -> options.putIfAbsent(option.getId(), option));
        }
        return options;
    }

    private List<OptionSelectionBySedeResponse> construirOpcionesPorSede(
            List<Ticket> tickets,
            Map<Long, String> sedeMap,
            Map<Long, Option> options) {
        return tickets.stream()
                .filter(ticket -> ticket.getSedeId() != null)
                .collect(Collectors.groupingBy(Ticket::getSedeId))
                .entrySet().stream()
                .map(entry -> {
                    int totalTickets = entry.getValue().size();
                    Map<Long, Long> counts = entry.getValue().stream()
                            .map(Ticket::getOptionId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(optionId -> optionId, Collectors.counting()));
                    List<OptionSelectionResponse> optionStats = counts.entrySet().stream()
                            .map(optionEntry -> {
                                Option selectedOption = options.get(optionEntry.getKey());
                                Option parentOption = selectedOption != null && selectedOption.getParentId() != null
                                        ? options.get(selectedOption.getParentId())
                                        : null;
                                int count = optionEntry.getValue().intValue();
                                double percentage = totalTickets > 0
                                        ? Math.round((count * 1000.0) / totalTickets) / 10.0
                                        : 0.0;
                                return OptionSelectionResponse.builder()
                                        .optionId(optionEntry.getKey())
                                        .categoryName(parentOption != null ? parentOption.getName() : null)
                                        .optionName(selectedOption != null
                                                ? selectedOption.getName()
                                                : "Opción #" + optionEntry.getKey())
                                        .count(count)
                                        .percentage(percentage)
                                        .build();
                            })
                            .sorted(Comparator.comparing(OptionSelectionResponse::getCount).reversed()
                                    .thenComparing(OptionSelectionResponse::getOptionName))
                            .collect(Collectors.toList());
                    return OptionSelectionBySedeResponse.builder()
                            .sedeId(entry.getKey())
                            .sedeName(sedeMap.getOrDefault(entry.getKey(), "Sede " + entry.getKey()))
                            .totalTickets(totalTickets)
                            .options(optionStats)
                            .build();
                })
                .sorted(Comparator.comparing(OptionSelectionBySedeResponse::getSedeName))
                .collect(Collectors.toList());
    }

    private List<TicketTraceabilityResponse> construirTrazabilidad(
            List<Ticket> reportTickets,
            Map<Long, String> sedeMap,
            Map<Long, List<QueueRating>> ratingsByTicketId,
            Map<Long, Option> options) {
        if (reportTickets.isEmpty()) return Collections.emptyList();

        Set<Long> ticketIds = reportTickets.stream().map(Ticket::getId).collect(Collectors.toSet());

        Set<Long> moduleIds = reportTickets.stream()
                .map(Ticket::getModuleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> modules = moduloAtencionRepository.findAllById(moduleIds).stream()
                .collect(Collectors.toMap(ModuloAtencion::getId, ModuloAtencion::getName));

        Set<Long> operatorIds = reportTickets.stream()
                .map(Ticket::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> operators = userRepository.findAllById(operatorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        Map<Long, List<QueueTicketHistory>> historyByTicket = ticketIds.isEmpty()
                ? Collections.emptyMap()
                : queueTicketHistoryRepository.findByTicketIdInOrderByCreatedAtAsc(ticketIds).stream()
                        .collect(Collectors.groupingBy(QueueTicketHistory::getTicketId));

        return reportTickets.stream()
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
