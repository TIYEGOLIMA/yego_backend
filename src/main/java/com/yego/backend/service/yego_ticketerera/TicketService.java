package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket.TicketStatus;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearTicketRequest;
import com.yego.backend.entity.yego_ticketerera.api.request.CompletarTicketRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWithCategoryResponse;

import java.util.List;
import java.util.Optional;

/**
 * Interface del servicio de Tickets del sistema YEGO Ticketerera
 */
public interface TicketService {
    
    Ticket crearTicket(CrearTicketRequest request);
    
    List<Ticket> obtenerTickets();
    
    List<Ticket> obtenerTicketsPorEstado(TicketStatus status);
    
    Ticket llamarTicket(Long ticketId, Long userId, Long moduleId);
    
    Ticket completarTicket(Long ticketId, CompletarTicketRequest request);
    
    Ticket cancelarTicket(Long ticketId, Long agentId);
    
    Ticket iniciarAtencion(Long ticketId, Long agentId);
    
    long contarTicketsPorEstado(TicketStatus status);

    List<Ticket> obtenerTicketsEnEsperaPorModulo(Long moduleId);

    long contarTicketsPorModuloYEstado(Long moduleId, TicketStatus status);

    long contarTicketsPorModuloYEstado(Long moduleId, String status);

    Ticket obtenerOAsignarTicketParaAgente(Long agentId);

    List<TicketWithCategoryResponse> convertirTicketsConCategorias(List<Ticket> tickets);

    TicketWithCategoryResponse convertirTicketConCategorias(Ticket ticket);

    /**
     * Lista todos los tickets activos. Si {@code sedeId} es {@code null} no se filtra por sede.
     */
    List<TicketWithCategoryResponse> obtenerTodosLosTicketsConCategorias(Long sedeId);

    /**
     * Lista los tickets de un estado dado. Si {@code sedeId} es {@code null} no se filtra por sede.
     */
    List<TicketWithCategoryResponse> obtenerTicketsPorEstado(String status, Long sedeId);

    /**
     * Cuenta los tickets de un estado dado. Si {@code sedeId} es {@code null} no se filtra por sede.
     */
    long contarTicketsPorEstado(String status, Long sedeId);
}
