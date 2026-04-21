package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearTicketRequest;
import com.yego.backend.entity.yego_ticketerera.api.request.CompletarTicketRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWithCategoryResponse;
import com.yego.backend.service.yego_ticketerera.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controlador REST para la gestión de tickets del sistema YEGO Ticketerera
 */
@RestController
@RequestMapping("/api/ticketera/tickets")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TicketController {
    
    private final TicketService ticketService;
    
    @PostMapping("/create")
   public ResponseEntity<Ticket> crearTicket(@Valid @RequestBody CrearTicketRequest request) {
        Ticket nuevoTicket = ticketService.crearTicket(request);
        return ResponseEntity.ok(nuevoTicket);
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<TicketWithCategoryResponse>> obtenerTickets(
            @RequestParam(required = false) Long sedeId) {
        List<TicketWithCategoryResponse> tickets = sedeId != null
                ? ticketService.obtenerTodosLosTicketsConCategoriasPorSede(sedeId)
                : ticketService.obtenerTodosLosTicketsConCategorias();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/waiting")
    public ResponseEntity<List<TicketWithCategoryResponse>> obtenerTicketsEnEspera(
            @RequestParam(required = false) Long sedeId) {
        List<TicketWithCategoryResponse> tickets = sedeId != null
                ? ticketService.obtenerTicketsPorEstadoYSede("WAITING", sedeId)
                : ticketService.obtenerTicketsEnEsperaConCategorias();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/called")
    public ResponseEntity<List<TicketWithCategoryResponse>> obtenerTicketsLlamados(
            @RequestParam(required = false) Long sedeId) {
        List<TicketWithCategoryResponse> tickets = sedeId != null
                ? ticketService.obtenerTicketsPorEstadoYSede("CALLED", sedeId)
                : ticketService.obtenerTicketsLlamadosConCategorias();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/in-progress")
    public ResponseEntity<List<TicketWithCategoryResponse>> obtenerTicketsEnProgreso(
            @RequestParam(required = false) Long sedeId) {
        List<TicketWithCategoryResponse> tickets = sedeId != null
                ? ticketService.obtenerTicketsPorEstadoYSede("IN_PROGRESS", sedeId)
                : ticketService.obtenerTicketsEnProgresoConCategorias();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<TicketWithCategoryResponse>> obtenerTicketsCompletados(
            @RequestParam(required = false) Long sedeId) {
        List<TicketWithCategoryResponse> tickets = sedeId != null
                ? ticketService.obtenerTicketsPorEstadoYSede("COMPLETED", sedeId)
                : ticketService.obtenerTicketsCompletadosConCategorias();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/stats/{status}")
    public ResponseEntity<Long> contarTicketsPorEstado(
            @PathVariable String status,
            @RequestParam(required = false) Long sedeId) {
        long count = sedeId != null
                ? ticketService.contarTicketsPorEstadoYSede(status, sedeId)
                : ticketService.contarTicketsPorEstado(status);
        return ResponseEntity.ok(count);
    }
    
    @PostMapping("/{ticketId}/call/{userId}")
    public ResponseEntity<Ticket> llamarTicket(
            @PathVariable Long ticketId, 
            @PathVariable Long userId,
            @RequestParam Long moduleId) {
        log.info("[Ticket] Llamar ticket {} por usuario {} en módulo {}", ticketId, userId, moduleId);
        Ticket ticket = ticketService.llamarTicket(ticketId, userId, moduleId);
        return ResponseEntity.ok(ticket);
    }
    
    @PostMapping("/{ticketId}/start/{agentId}")
    public ResponseEntity<Ticket> iniciarAtencion(@PathVariable Long ticketId, @PathVariable Long agentId) {
        Ticket ticket = ticketService.iniciarAtencion(ticketId, agentId);
        return ResponseEntity.ok(ticket);
    }
    
    @PostMapping("/{ticketId}/complete")
    public ResponseEntity<Ticket> completarTicket(@PathVariable Long ticketId, @Valid @RequestBody CompletarTicketRequest request) {
        Ticket ticket = ticketService.completarTicket(ticketId, request);
        return ResponseEntity.ok(ticket);
    }
    
    @PostMapping("/{ticketId}/cancel/{agentId}")
    public ResponseEntity<Ticket> cancelarTicket(@PathVariable Long ticketId, @PathVariable Long agentId) {
        Ticket ticket = ticketService.cancelarTicket(ticketId, agentId);
        return ResponseEntity.ok(ticket);
    }
    
    @GetMapping("/agent/{agentId}/assigned")
    public ResponseEntity<Ticket> obtenerOAsignarTicketParaAgente(@PathVariable Long agentId) {
        Ticket ticket = ticketService.obtenerOAsignarTicketParaAgente(agentId);
        if (ticket != null) {
            return ResponseEntity.ok(ticket);
        } else {
            return ResponseEntity.noContent().build();
        }
    }
    
    @GetMapping("/module/{moduleId}/waiting")
    public ResponseEntity<List<Ticket>> obtenerTicketsEnEsperaPorModulo(@PathVariable Long moduleId) {
        List<Ticket> tickets = ticketService.obtenerTicketsEnEsperaPorModulo(moduleId);
        return ResponseEntity.ok(tickets);
    }
    
    @GetMapping("/module/{moduleId}/stats/{status}")
    public ResponseEntity<Long> contarTicketsPorModuloYEstado(@PathVariable Long moduleId, @PathVariable String status) {
        long count = ticketService.contarTicketsPorModuloYEstado(moduleId, status);
        return ResponseEntity.ok(count);
    }
}
