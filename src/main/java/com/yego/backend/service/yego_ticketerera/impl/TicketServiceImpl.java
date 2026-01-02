package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket.TicketStatus;
import com.yego.backend.entity.yego_ticketerera.entities.Option;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearTicketRequest;
import com.yego.backend.entity.yego_ticketerera.api.request.CompletarTicketRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWithCategoryResponse;
import com.yego.backend.repository.yego_ticketerera.TicketRepository;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import com.yego.backend.service.yego_ticketerera.TicketService;
import com.yego.backend.service.yego_ticketerera.QueueTicketHistoryService;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import com.yego.backend.handler.yego_ticketerera.TicketNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación del servicio de Tickets del sistema YEGO Ticketerera
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final OptionRepository optionRepository;
    private final TicketNotificationHandler ticketNotificationHandler;
    private final QueueTicketHistoryService queueTicketHistoryService;
    private final QueueAgentService queueAgentService;
    
    // Cache para estadísticas
    private final ConcurrentHashMap<TicketStatus, Long> statsCache = new ConcurrentHashMap<>();
    private volatile long lastStatsUpdate = 0;
    private static final long STATS_CACHE_TTL = 5000;
    
    @Override
    @Transactional
    public Ticket crearTicket(CrearTicketRequest request) {
        if (request.getOptionId() == null) {
            throw new IllegalArgumentException("El optionId es requerido");
        }
        
        Ticket ticket = Ticket.builder()
                .optionId(request.getOptionId())
                .licenseNumber(request.getLicenseNumber())
                .userId(null) // Se asigna cuando el agente toma el ticket
                .moduleId(null)
                .status(TicketStatus.WAITING)
                .priority(1)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Lima")))
                .build();
        
        String ticketNumber = generarNumeroTicket(ticket.getOptionId());
        ticket.setTicketNumber(ticketNumber);
        
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket creado: {} (sin asignar agente aún)", savedTicket.getTicketNumber());
        
        // Enviar notificación WebSocket
        ticketNotificationHandler.enviarNuevoTicket(savedTicket);
        
        // Limpiar caches relacionados
        limpiarCaches();
        
        return savedTicket;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Ticket> obtenerTickets() {
        return ticketRepository.findActiveTickets();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Ticket> obtenerTicketsPorEstado(TicketStatus status) {
        return ticketRepository.findByStatusOrderByCreatedAtAsc(status);
    }
    
    @Override
    @Transactional
    public Ticket llamarTicket(Long ticketId, Long userId, Long moduleId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        // Guardar estado anterior
        TicketStatus estadoAnterior = ticket.getStatus();
        
        // Obtener el agentId del userId
        Optional<Long> queueAgentIdOpt = queueAgentService.obtenerQueueAgentIdPorUsuario(userId);
        Long agentId = queueAgentIdOpt.orElseThrow(() -> new RuntimeException("No se encontró agentId para el usuario: " + userId));
        
        ticket.setStatus(TicketStatus.CALLED);
        ticket.setUserId(userId); // Asignar userId al ticket
        ticket.setModuleId(moduleId); // Asignar módulo al llamar
        ticket.setCalledAt(LocalDateTime.now(ZoneId.of("America/Lima")));
        
        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Ticket llamado: {} por usuario: {} (agentId: {}) para módulo: {}", updatedTicket.getTicketNumber(), userId, agentId, moduleId);
        
        // Registrar en historial
        try {
            queueTicketHistoryService.registrarCambioEstado(
                ticketId,
                agentId,
                estadoAnterior.name(),
                "CALLED",
                "Ticket llamado por el agente"
            );
        } catch (Exception e) {
            log.error("Error registrando historial para ticket llamado {}: {}", ticketId, e.getMessage());
        }
        
        // Enviar notificación WebSocket
        ticketNotificationHandler.enviarTicketLlamado(convertirTicketConCategorias(updatedTicket));
        
        return updatedTicket;
    }
    
    @Override
    @Transactional
    public Ticket completarTicket(Long ticketId, CompletarTicketRequest request) {
        log.info("Completando ticket {} por agente {}", ticketId, request.getAgentId());
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado con ID: " + ticketId));
        
        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException("Solo se pueden completar tickets que estén en proceso");
        }
        
        // Guardar estado anterior
        TicketStatus estadoAnterior = ticket.getStatus();
        
        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(LocalDateTime.now(ZoneId.of("America/Lima")));
        
        Ticket ticketGuardado = ticketRepository.save(ticket);
        log.info("Ticket {} completado exitosamente", ticketId);

         // Preparar notas (si están vacías, usar mensaje por defecto)
         String notas = (request.getNotes() != null && !request.getNotes().trim().isEmpty()) 
         ? request.getNotes().trim()
         : "Ticket completado sin notas adicionales";
        
        // Registrar en historial
        try {
            Optional<Long> queueAgentIdOpt = queueAgentService.obtenerQueueAgentIdPorUsuario(request.getAgentId());
            Long queueAgentId = queueAgentIdOpt.orElse(null);
            
            queueTicketHistoryService.registrarCambioEstado(
                ticketId,
                queueAgentId,
                estadoAnterior.name(),
                "COMPLETED",
                notas
            );
        } catch (Exception e) {
            log.error("Error registrando historial para ticket completado {}: {}", ticketId, e.getMessage());
        }
        ticketNotificationHandler.enviarTicketCompletado(convertirTicketConCategorias(ticketGuardado));
        
        return ticketGuardado;
    }
    
    @Override
    @Transactional
    public Ticket cancelarTicket(Long ticketId, Long agentId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        ticket.setStatus(TicketStatus.CANCELLED);
        
        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Ticket cancelado: {} por agente: {}", updatedTicket.getTicketNumber(), agentId);
        
        // Enviar notificación WebSocket
        ticketNotificationHandler.enviarTicketCancelado(updatedTicket);
        
        return updatedTicket;
    }
    
    @Override
    @Transactional
    public Ticket iniciarAtencion(Long ticketId, Long agentId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        // Guardar estado anterior
        TicketStatus estadoAnterior = ticket.getStatus();
        
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        
        Ticket updatedTicket = ticketRepository.save(ticket);
        log.info("Atención iniciada para ticket: {} por agente: {}", updatedTicket.getTicketNumber(), agentId);
        
        // Registrar en historial
        try {
            Optional<Long> queueAgentIdOpt = queueAgentService.obtenerQueueAgentIdPorUsuario(agentId);
            Long queueAgentId = queueAgentIdOpt.orElse(null);
            
            queueTicketHistoryService.registrarCambioEstado(
                ticketId,
                queueAgentId,
                estadoAnterior.name(),
                "IN_PROGRESS",
                "Atención iniciada por el agente"
            );
        } catch (Exception e) {
            log.error("Error registrando historial para ticket en progreso {}: {}", ticketId, e.getMessage());
        }
        
        // Enviar notificación WebSocket
        ticketNotificationHandler.enviarTicketIniciado(convertirTicketConCategorias(updatedTicket));
        
        return updatedTicket;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> obtenerUltimoTicketLlamado() {
        return ticketRepository.findLastCalledTicket();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long contarTicketsPorEstado(TicketStatus status) {
        long currentTime = System.currentTimeMillis();
        
        // Usar cache si no ha expirado
        if (currentTime - lastStatsUpdate < STATS_CACHE_TTL && statsCache.containsKey(status)) {
            return statsCache.get(status);
        }
        
        // Si el cache expiró, actualizar todas las estadísticas
        if (currentTime - lastStatsUpdate >= STATS_CACHE_TTL) {
            actualizarCacheEstadisticas();
            lastStatsUpdate = currentTime;
        }
        
        return statsCache.getOrDefault(status, 0L);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Ticket> obtenerTicketsEnEsperaPorModulo(Long moduleId) {
        log.info("Obteniendo tickets en espera para el módulo: {}", moduleId);
        return ticketRepository.findWaitingTicketsByModuleOrdered(moduleId, TicketStatus.WAITING);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long contarTicketsPorModuloYEstado(Long moduleId, TicketStatus status) {
        log.info("Contando tickets del módulo {} con estado: {}", moduleId, status);
        return ticketRepository.countByModuleIdAndStatus(moduleId, status);
    }
    
    @Override
    @Transactional
    public Ticket obtenerOAsignarTicketParaAgente(Long agentId) {
        log.info("🎯 Buscando ticket para agente: {}", agentId);
        
        // 1. Verificar si el agente ya tiene un ticket asignado
        List<Ticket.TicketStatus> estadosActivos = Arrays.asList(
            Ticket.TicketStatus.CALLED,
            Ticket.TicketStatus.IN_PROGRESS
        );
        
        List<Ticket> ticketsExistentes = ticketRepository.findByAgentIdAndStatusIn(agentId, estadosActivos);
        Optional<Ticket> ticketExistente = ticketsExistentes.stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .findFirst();
        
        if (ticketExistente.isPresent()) {
            log.info("✅ Agente {} ya tiene ticket asignado: {}", agentId, ticketExistente.get().getTicketNumber());
            return ticketExistente.get();
        }
        
        // 2. Si no tiene ticket, buscar el próximo disponible
        Optional<Ticket> proximoTicket = ticketRepository.findFirstAvailableTicket(Ticket.TicketStatus.WAITING);
        
        if (proximoTicket.isPresent()) {
            Ticket ticket = proximoTicket.get();
            
            // 3. Asignar el ticket al agente
            ticket.setAgentId(agentId);
            ticket.setStatus(Ticket.TicketStatus.WAITING);
            ticket.setCalledAt(LocalDateTime.now(ZoneId.of("America/Lima")));
            
            Ticket ticketAsignado = ticketRepository.save(ticket);
            
            log.info("🚀 Ticket {} asignado automáticamente al agente {}", 
                ticket.getTicketNumber(), agentId);
            return ticketAsignado;
        }
        
        // 3. No hay tickets disponibles
        log.info("📭 No hay tickets disponibles para asignar al agente {}", agentId);
        return null;
    }
    
    @Override
    public TicketWithCategoryResponse convertirTicketConCategorias(Ticket ticket) {
        String categoryName = null;
        String subcategoryName = null;
        String categoryDescription = null;
        String subcategoryDescription = null;
        
        if (ticket.getOptionId() != null) {
            try {
                Option option = optionRepository.findById(ticket.getOptionId()).orElse(null);
                if (option != null) {
                    subcategoryName = option.getName();
                    subcategoryDescription = option.getDescription();
                    
                    // Obtener la categoría (opción padre)
                    if (option.getParentId() != null) {
                        Option parentOption = optionRepository.findById(option.getParentId()).orElse(null);
                        if (parentOption != null) {
                            categoryName = parentOption.getName();
                            categoryDescription = parentOption.getDescription();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error obteniendo información de categorías para ticket {}: {}", ticket.getId(), e.getMessage());
            }
        }
        
        return TicketWithCategoryResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .userId(ticket.getUserId())
                .optionId(ticket.getOptionId())
                .moduleId(ticket.getModuleId())
                .agentId(ticket.getAgentId())
                .licenseNumber(ticket.getLicenseNumber())
                .status(ticket.getStatus().name())
                .priority(ticket.getPriority())
                .createdAt(ticket.getCreatedAt())
                .calledAt(ticket.getCalledAt())
                .completedAt(ticket.getCompletedAt())
                .categoryName(categoryName)
                .subcategoryName(subcategoryName)
                .categoryDescription(categoryDescription)
                .subcategoryDescription(subcategoryDescription)
                .build();
    }
    
    @Override
    public List<TicketWithCategoryResponse> convertirTicketsConCategorias(List<Ticket> tickets) {
        return tickets.stream()
                .map(this::convertirTicketConCategorias)
                .collect(java.util.stream.Collectors.toList());
    }
    
    // Métodos privados auxiliares
    
    private void actualizarCacheEstadisticas() {
        log.debug("Actualizando cache de estadísticas");
        for (TicketStatus status : TicketStatus.values()) {
            long count = ticketRepository.countByStatus(status);
            statsCache.put(status, count);
        }
    }
    
    private void limpiarCaches() {
        log.debug("Limpiando caches");
        statsCache.clear();
        lastStatsUpdate = 0;
    }
    
    private String generarNumeroTicket(Long optionId) {
        // Obtener el ID del módulo desde la opción
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Opción no encontrada"));
        
        Long moduleId = option.getModuleId();
        if (moduleId == null) {
            throw new IllegalArgumentException("La opción debe tener un módulo asignado");
        }
        
        String ticketNumber;
        int intentos = 0;
        
        do {
            // Obtener el siguiente número consecutivo para este módulo (máximo 9)
            long consecutivo = (ticketRepository.countByModuleId(moduleId) % 9) + 1;
            
            // Obtener la hora actual (hora, minutos, segundos) - Zona horaria de Perú
            LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Lima"));
            int hora = now.getHour();
            int minutos = now.getMinute();
            int segundos = now.getSecond();
            
            // Formato: M + módulo(1 dígito) + consecutivo(1 dígito) + hora(2 dígitos) + minutos(2 dígitos) + segundos(2 dígitos)
            ticketNumber = String.format("M%d%d%02d%02d%02d", moduleId, consecutivo, hora, minutos, segundos);
            
            intentos++;
            
            // Máximo 50 intentos para evitar loop infinito
            if (intentos > 50) {
                // Fallback: usar timestamp en lugar de aleatorios
                long timestamp = System.currentTimeMillis() % 1000;
                ticketNumber = String.format("M%d%d%03d", moduleId, consecutivo, timestamp);
                break;
            }
            
        } while (ticketRepository.existsByTicketNumber(ticketNumber));
        
        log.info("Número de ticket generado: {} para módulo {} (intentos: {})", ticketNumber, moduleId, intentos);
        return ticketNumber;
    }
    
    // Métodos simplificados para el controlador (sin lógica de negocio en el controlador)
    
    @Override
    public List<TicketWithCategoryResponse> obtenerTodosLosTicketsConCategorias() {
        log.info("Obteniendo todos los tickets con categorías");
        List<Ticket> tickets = obtenerTickets();
        return convertirTicketsConCategorias(tickets);
    }
    
    @Override
    public List<TicketWithCategoryResponse> obtenerTicketsEnEsperaConCategorias() {
        log.info("Obteniendo tickets en espera con categorías");
        List<Ticket> tickets = obtenerTicketsPorEstado(Ticket.TicketStatus.WAITING);
        return convertirTicketsConCategorias(tickets);
    }
    
    @Override
    public List<TicketWithCategoryResponse> obtenerTicketsLlamadosConCategorias() {
        log.info("Obteniendo tickets llamados con categorías");
        List<Ticket> tickets = obtenerTicketsPorEstado(Ticket.TicketStatus.CALLED);
        return convertirTicketsConCategorias(tickets);
    }
    
    @Override
    public List<TicketWithCategoryResponse> obtenerTicketsEnProgresoConCategorias() {
        log.info("Obteniendo tickets en progreso con categorías");
        List<Ticket> tickets = obtenerTicketsPorEstado(Ticket.TicketStatus.IN_PROGRESS);
        return convertirTicketsConCategorias(tickets);
    }
    
    @Override
    public List<TicketWithCategoryResponse> obtenerTicketsCompletadosConCategorias() {
        log.info("Obteniendo tickets completados con categorías");
        List<Ticket> tickets = obtenerTicketsPorEstado(Ticket.TicketStatus.COMPLETED);
        return convertirTicketsConCategorias(tickets);
    }
    
    @Override
    public long contarTicketsPorEstado(String status) {
        log.info("Contando tickets por estado: {}", status);
        Ticket.TicketStatus ticketStatus = Ticket.TicketStatus.valueOf(status.toUpperCase());
        return contarTicketsPorEstado(ticketStatus);
    }
    
    @Override
    public long contarTicketsPorModuloYEstado(Long moduleId, String status) {
        log.info("Contando tickets del módulo {} con estado: {}", moduleId, status);
        Ticket.TicketStatus ticketStatus = Ticket.TicketStatus.valueOf(status.toUpperCase());
        return contarTicketsPorModuloYEstado(moduleId, ticketStatus);
    }
}
