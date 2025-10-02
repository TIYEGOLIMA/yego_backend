package com.yego.backend.service;

import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWithCategoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Servicio WebSocket centralizado - Basado en ticketera_backend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Enviar nuevo ticket creado
     */
    public void enviarNuevoTicket(Ticket ticket) {
        log.info("📤 Enviando nuevo ticket por WebSocket: {}", ticket.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/tickets", ticket);
        messagingTemplate.convertAndSend("/topic/new-ticket", ticket);
    }
    
    /**
     * Enviar ticket llamado
     */
    public void enviarTicketLlamado(TicketWithCategoryResponse ticketCompleto) {
        log.info("📤 Enviando ticket llamado por WebSocket: {}", ticketCompleto.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/tickets", ticketCompleto);
        messagingTemplate.convertAndSend("/topic/ticket-called", ticketCompleto);
    }
    
    /**
     * Enviar ticket iniciado
     */
    public void enviarTicketIniciado(TicketWithCategoryResponse ticketCompleto) {
        log.info("📤 Enviando ticket iniciado por WebSocket: {}", ticketCompleto.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/tickets", ticketCompleto);
        messagingTemplate.convertAndSend("/topic/ticket-started", ticketCompleto);
    }
    
    /**
     * Enviar ticket completado
     */
    public void enviarTicketCompletado(TicketWithCategoryResponse ticketCompleto) {
        log.info("📤 Enviando ticket completado por WebSocket: {}", ticketCompleto.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/tickets", ticketCompleto);
        messagingTemplate.convertAndSend("/topic/ticket-completed", ticketCompleto);
    }
    
    /**
     * Enviar ticket cancelado
     */
    public void enviarTicketCancelado(Ticket ticket) {
        log.info("📤 Enviando ticket cancelado por WebSocket: {}", ticket.getTicketNumber());
        messagingTemplate.convertAndSend("/topic/tickets", ticket);
        messagingTemplate.convertAndSend("/topic/ticket-cancelled", ticket);
    }
    
    /**
     * LEGACY: Mantener compatibilidad con método anterior
     */
    public void sendTicketeraEvent(String event, Object data) {
        Map<String, Object> notification = Map.of(
            "type", event,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/ticketera", notification);
        log.info("📤 [WebSocket] Ticketera: {} - Enviado a /topic/ticketera", event);
    }
    
    /**
     * Enviar notificación a OKR
     */
    public void sendOkrEvent(String event, Object data) {
        Map<String, Object> notification = Map.of(
            "event", event,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/okr", notification);
        log.info("📤 [WebSocket] OKR: {}", event);
    }
    
    /**
     * Enviar notificación a Marketing
     */
    public void sendMarketingEvent(String event, Object data) {
        Map<String, Object> notification = Map.of(
            "event", event,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/marketing", notification);
        log.info("📤 [WebSocket] Marketing: {}", event);
    }
    
    /**
     * Enviar notificación global del sistema
     */
    public void sendSystemEvent(String event, Object data) {
        Map<String, Object> notification = Map.of(
            "event", event,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/system", notification);
        log.info("📤 [WebSocket] Sistema: {}", event);
    }
    
}
