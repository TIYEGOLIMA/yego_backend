package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.request.CompletarTicketRequest;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearTicketRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.TicketWithCategoryResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.service.yego_ticketerera.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticketera/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/create")
    public ResponseEntity<Ticket> crearTicket(@Valid @RequestBody CrearTicketRequest request) {
        return ResponseEntity.ok(ticketService.crearTicket(request));
    }

    @GetMapping("/all")
    public ResponseEntity<List<TicketWithCategoryResponse>> obtenerTickets(
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(ticketService.obtenerTodosLosTicketsConCategorias(sedeId));
    }

    @GetMapping("/waiting")
    public ResponseEntity<List<TicketWithCategoryResponse>> obtenerTicketsEnEspera(
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorEstado("WAITING", sedeId));
    }

    @GetMapping("/called")
    public ResponseEntity<List<TicketWithCategoryResponse>> obtenerTicketsLlamados(
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorEstado("CALLED", sedeId));
    }

    @GetMapping("/in-progress")
    public ResponseEntity<List<TicketWithCategoryResponse>> obtenerTicketsEnProgreso(
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorEstado("IN_PROGRESS", sedeId));
    }

    @GetMapping("/completed")
    public ResponseEntity<List<TicketWithCategoryResponse>> obtenerTicketsCompletados(
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsPorEstado("COMPLETED", sedeId));
    }

    @GetMapping("/stats/{status}")
    public ResponseEntity<Long> contarTicketsPorEstado(
            @PathVariable String status,
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(ticketService.contarTicketsPorEstado(status, sedeId));
    }

    @PostMapping("/{ticketId}/call/{userId}")
    public ResponseEntity<Ticket> llamarTicket(@PathVariable Long ticketId,
                                               @PathVariable Long userId,
                                               @RequestParam Long moduleId) {
        return ResponseEntity.ok(ticketService.llamarTicket(ticketId, userId, moduleId));
    }

    @PostMapping("/{ticketId}/start/{agentId}")
    public ResponseEntity<Ticket> iniciarAtencion(@PathVariable Long ticketId, @PathVariable Long agentId) {
        return ResponseEntity.ok(ticketService.iniciarAtencion(ticketId, agentId));
    }

    @PostMapping("/{ticketId}/complete")
    public ResponseEntity<Ticket> completarTicket(@PathVariable Long ticketId,
                                                  @Valid @RequestBody CompletarTicketRequest request) {
        return ResponseEntity.ok(ticketService.completarTicket(ticketId, request));
    }

    @PostMapping("/{ticketId}/cancel/{agentId}")
    public ResponseEntity<Ticket> cancelarTicket(@PathVariable Long ticketId, @PathVariable Long agentId) {
        return ResponseEntity.ok(ticketService.cancelarTicket(ticketId, agentId));
    }

    @GetMapping("/agent/{agentId}/assigned")
    public ResponseEntity<Ticket> obtenerOAsignarTicketParaAgente(@PathVariable Long agentId) {
        Ticket ticket = ticketService.obtenerOAsignarTicketParaAgente(agentId);
        return ticket != null ? ResponseEntity.ok(ticket) : ResponseEntity.noContent().build();
    }

    @GetMapping("/module/{moduleId}/waiting")
    public ResponseEntity<List<Ticket>> obtenerTicketsEnEsperaPorModulo(@PathVariable Long moduleId) {
        return ResponseEntity.ok(ticketService.obtenerTicketsEnEsperaPorModulo(moduleId));
    }

    @GetMapping("/module/{moduleId}/stats/{status}")
    public ResponseEntity<Long> contarTicketsPorModuloYEstado(@PathVariable Long moduleId,
                                                              @PathVariable String status) {
        return ResponseEntity.ok(ticketService.contarTicketsPorModuloYEstado(moduleId, status));
    }
}
