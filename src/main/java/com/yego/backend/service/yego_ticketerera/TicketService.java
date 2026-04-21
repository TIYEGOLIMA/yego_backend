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
    
    // Método simplificado para el controlador
    long contarTicketsPorEstado(String status);
    
    List<Ticket> obtenerTicketsEnEsperaPorModulo(Long moduleId);
    
    long contarTicketsPorModuloYEstado(Long moduleId, TicketStatus status);
    
    // Método simplificado para el controlador
    long contarTicketsPorModuloYEstado(Long moduleId, String status);
    
    // Asignación automática de tickets para agentes
    Ticket obtenerOAsignarTicketParaAgente(Long agentId);
    
    // Convertir tickets con información de categorías
    List<TicketWithCategoryResponse> convertirTicketsConCategorias(List<Ticket> tickets);
    
    TicketWithCategoryResponse convertirTicketConCategorias(Ticket ticket);
    
    // Métodos simplificados para el controlador (sin lógica de negocio en el controlador)
    List<TicketWithCategoryResponse> obtenerTodosLosTicketsConCategorias();
    
    List<TicketWithCategoryResponse> obtenerTicketsEnEsperaConCategorias();
    
    List<TicketWithCategoryResponse> obtenerTicketsLlamadosConCategorias();
    
    List<TicketWithCategoryResponse> obtenerTicketsEnProgresoConCategorias();
    
    List<TicketWithCategoryResponse> obtenerTicketsCompletadosConCategorias();

    // Métodos filtrados por sede
    List<TicketWithCategoryResponse> obtenerTodosLosTicketsConCategoriasPorSede(Long sedeId);

    List<TicketWithCategoryResponse> obtenerTicketsPorEstadoYSede(String status, Long sedeId);

    long contarTicketsPorEstadoYSede(String status, Long sedeId);
}
