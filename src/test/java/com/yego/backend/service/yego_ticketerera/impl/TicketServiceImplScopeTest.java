package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearTicketRequest;
import com.yego.backend.entity.yego_ticketerera.entities.Option;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.handler.yego_ticketerera.TicketNotificationHandler;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import com.yego.backend.repository.yego_ticketerera.TicketRepository;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import com.yego.backend.service.yego_ticketerera.QueueTicketHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplScopeTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private OptionRepository optionRepository;
    @Mock
    private TicketNotificationHandler ticketNotificationHandler;
    @Mock
    private QueueTicketHistoryService queueTicketHistoryService;
    @Mock
    private QueueAgentService queueAgentService;
    @InjectMocks
    private TicketServiceImpl service;

    @Test
    void crearTicketConservaLaSedeYNotificaEseMismoTicket() {
        CrearTicketRequest request = new CrearTicketRequest();
        request.setOptionId(5L);
        request.setLicenseNumber("+51999999999");
        request.setSedeId(10L);

        when(optionRepository.findById(5L)).thenReturn(Optional.of(Option.builder().id(5L).name("Soporte").build()));
        when(ticketRepository.count()).thenReturn(0L);
        when(ticketRepository.existsByTicketNumber(any())).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(51L);
            return saved;
        });

        Ticket created = service.crearTicket(request);

        assertThat(created.getId()).isEqualTo(51L);
        assertThat(created.getSedeId()).isEqualTo(10L);
        assertThat(created.getStatus()).isEqualTo(Ticket.TicketStatus.WAITING);
        verify(ticketNotificationHandler).enviarNuevoTicket(created);
    }

    @Test
    void listadoDeSedeNoConsultaNiDevuelveElListadoGlobal() {
        Ticket sedeTicket = Ticket.builder()
                .id(51L)
                .ticketNumber("L-051")
                .status(Ticket.TicketStatus.WAITING)
                .priority(1)
                .sedeId(10L)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
        when(ticketRepository.findActiveTicketsBySede(10L)).thenReturn(List.of(sedeTicket));

        var result = service.obtenerTodosLosTicketsConCategorias(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSedeId()).isEqualTo(10L);
        verify(ticketRepository).findActiveTicketsBySede(10L);
        verify(ticketRepository, never()).findActiveTickets();
    }
}
