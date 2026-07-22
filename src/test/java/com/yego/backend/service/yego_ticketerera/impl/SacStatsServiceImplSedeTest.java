package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Sede;
import com.yego.backend.entity.yego_ticketerera.entities.Option;
import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.repository.yego_ticketerera.QueueRatingRepository;
import com.yego.backend.repository.yego_ticketerera.QueueTicketHistoryRepository;
import com.yego.backend.repository.yego_ticketerera.SedeRepository;
import com.yego.backend.repository.yego_ticketerera.TicketRepository;
import com.yego.backend.repository.yego_ticketerera.ModuloAtencionRepository;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SacStatsServiceImplSedeTest {

    @Mock private UserRepository userRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private QueueRatingRepository queueRatingRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private OptionRepository optionRepository;
    @Mock private ModuloAtencionRepository moduloAtencionRepository;
    @Mock private QueueTicketHistoryRepository queueTicketHistoryRepository;

    private SacStatsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SacStatsServiceImpl(
                userRepository, ticketRepository, queueRatingRepository, sedeRepository,
                optionRepository, moduloAtencionRepository, queueTicketHistoryRepository);
        when(sedeRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(
                Sede.builder().id(10L).name("Lima").build(),
                Sede.builder().id(20L).name("Callao").build()));
    }

    @Test
    void filtraTicketsPorSedeAunqueElOperadorHayaCambiado() {
        User operador = operador(1L, 20L);
        Ticket ticketLima = ticket(101L, 1L, 10L);

        when(userRepository.findByRoleName("SAC")).thenReturn(List.of(operador));
        when(ticketRepository.countBySedeId(10L)).thenReturn(1L);
        when(queueRatingRepository.countBySedeId(10L)).thenReturn(0L);
        when(queueRatingRepository.getAverageRatingBySedeId(10L)).thenReturn(0.0);
        when(ticketRepository.findBySedeId(10L)).thenReturn(List.of(ticketLima));

        SacStatsResponse response = service.obtenerTodasLasEstadisticas(null, null, 10L);

        assertThat(response.getTotalTickets()).isEqualTo(1);
        assertThat(response.getSacPerformance()).singleElement().satisfies(performance -> {
            assertThat(performance.getSedeId()).isEqualTo(10L);
            assertThat(performance.getSedeName()).isEqualTo("Lima");
            assertThat(performance.getTotalTickets()).isEqualTo(1);
            assertThat(performance.getResolutionPercentage()).isEqualTo(100.0);
            assertThat(performance.getAverageServiceTime()).isEqualTo("4 min");
        });
        assertThat(response.getHourlyBySede()).extracting(SacStatsResponse.HourlyBySede::getSedeId)
                .containsExactly(10L);
    }

    @Test
    void separaElHistorialDelMismoOperadorPorLaSedeRealDeCadaTicket() {
        User operador = operador(1L, 20L);
        Ticket ticketLima = ticket(101L, 1L, 10L);
        ticketLima.setOptionId(12L);
        Ticket ticketCallao = ticket(202L, 1L, 20L);
        ticketCallao.setOptionId(21L);
        when(userRepository.findByRoleName("SAC")).thenReturn(List.of(operador));
        when(ticketRepository.count()).thenReturn(2L);
        when(queueRatingRepository.count()).thenReturn(0L);
        when(queueRatingRepository.getAverageRating()).thenReturn(0.0);
        when(ticketRepository.findAll()).thenReturn(List.of(ticketLima, ticketCallao));
        when(optionRepository.findAllById(eq(java.util.Set.of(12L, 21L)))).thenReturn(List.of(
                Option.builder().id(12L).name("Actualizar datos").build(),
                Option.builder().id(21L).name("Consultar liquidación").build()));

        SacStatsResponse response = service.obtenerTodasLasEstadisticas(null, null, null);

        assertThat(response.getSacPerformance())
                .extracting(SacStatsResponse.SacPerformanceResponse::getSedeId)
                .containsExactly(10L, 20L);
        assertThat(response.getSacPerformance())
                .extracting(SacStatsResponse.SacPerformanceResponse::getTotalTickets)
                .containsExactly(1, 1);
        assertThat(response.getOptionSelectionsBySede())
                .extracting(SacStatsResponse.OptionSelectionBySedeResponse::getSedeName)
                .containsExactly("Callao", "Lima");
        assertThat(response.getOptionSelectionsBySede().get(0).getOptions())
                .extracting(SacStatsResponse.OptionSelectionResponse::getOptionName)
                .containsExactly("Consultar liquidación");
        assertThat(response.getOptionSelectionsBySede().get(1).getOptions())
                .extracting(SacStatsResponse.OptionSelectionResponse::getOptionName)
                .containsExactly("Actualizar datos");
    }

    @Test
    void combinaFiltroDeFechaYSedeParaLosTicketsDelReporte() {
        User operador = operador(1L, 10L);
        when(userRepository.findByRoleName("SAC")).thenReturn(List.of(operador));
        when(ticketRepository.countBySedeIdAndCreatedAtBetween(eq(10L), any(), any())).thenReturn(0L);
        when(queueRatingRepository.countBySedeIdAndCreatedAtBetween(eq(10L), any(), any())).thenReturn(0L);
        when(queueRatingRepository.getAverageRatingBySedeIdAndDateRange(eq(10L), any(), any())).thenReturn(0.0);
        when(ticketRepository.findBySedeIdAndCreatedAtBetween(eq(10L), any(), any()))
                .thenReturn(List.of());
        service.obtenerTodasLasEstadisticas("2026-07-01", "2026-07-16", 10L);

        verify(ticketRepository).findBySedeIdAndCreatedAtBetween(
                eq(10L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void muestraSedeOpcionMarcadaYRecorridoDelTicket() {
        User operador = operador(1L, 10L);
        Ticket ticket = ticket(101L, 1L, 10L);
        ticket.setOptionId(12L);

        when(userRepository.findByRoleName("SAC")).thenReturn(List.of(operador));
        when(userRepository.findAllById(eq(java.util.Set.of(1L)))).thenReturn(List.of(operador));
        when(ticketRepository.countBySedeId(10L)).thenReturn(1L);
        when(queueRatingRepository.countBySedeId(10L)).thenReturn(0L);
        when(queueRatingRepository.getAverageRatingBySedeId(10L)).thenReturn(0.0);
        when(ticketRepository.findBySedeId(10L)).thenReturn(List.of(ticket));
        when(optionRepository.findAllById(eq(java.util.Set.of(12L)))).thenReturn(List.of(
                Option.builder().id(12L).name("Actualización de datos").parentId(5L).build()));
        when(optionRepository.findAllById(eq(java.util.Set.of(5L)))).thenReturn(List.of(
                Option.builder().id(5L).name("Cuenta del conductor").build()));
        when(queueTicketHistoryRepository.findByTicketIdInOrderByCreatedAtAsc(eq(java.util.Set.of(101L))))
                .thenReturn(List.of(QueueTicketHistory.builder()
                        .ticketId(101L)
                        .newStatus("COMPLETED")
                        .createdAt(ticket.getCompletedAt())
                        .notes("Atención finalizada")
                        .build()));

        SacStatsResponse response = service.obtenerTodasLasEstadisticas(null, null, 10L);

        assertThat(response.getTraceabilityTotal()).isEqualTo(1);
        assertThat(response.getOptionSelectionsBySede()).singleElement().satisfies(sedeStats -> {
            assertThat(sedeStats.getSedeId()).isEqualTo(10L);
            assertThat(sedeStats.getTotalTickets()).isEqualTo(1);
            assertThat(sedeStats.getOptions()).singleElement().satisfies(option -> {
                assertThat(option.getCategoryName()).isEqualTo("Cuenta del conductor");
                assertThat(option.getOptionName()).isEqualTo("Actualización de datos");
                assertThat(option.getCount()).isEqualTo(1);
                assertThat(option.getPercentage()).isEqualTo(100.0);
            });
        });
        assertThat(response.getTicketTraceability()).singleElement().satisfies(trace -> {
            assertThat(trace.getSedeId()).isEqualTo(10L);
            assertThat(trace.getSedeName()).isEqualTo("Lima");
            assertThat(trace.getCategoryName()).isEqualTo("Cuenta del conductor");
            assertThat(trace.getOptionName()).isEqualTo("Actualización de datos");
            assertThat(trace.getOperatorName()).isEqualTo("Operador");
            assertThat(trace.getEvents())
                    .extracting(SacStatsResponse.TicketTraceEventResponse::getStatus)
                    .containsExactly("GENERATED", "COMPLETED");
        });
    }

    @Test
    void paginaLaTrazabilidadPorSedeEnOrdenMasReciente() {
        Ticket ticket = ticket(101L, 1L, 10L);
        Pageable requestedPage = PageRequest.of(1, 10);
        when(ticketRepository.findBySedeId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket), requestedPage, 25));

        var response = service.obtenerTrazabilidad(null, null, 10L, 1, 10);

        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(25);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getContent()).singleElement().satisfies(trace -> {
            assertThat(trace.getTicketNumber()).isEqualTo("T-101");
            assertThat(trace.getSedeName()).isEqualTo("Lima");
        });
        verify(ticketRepository).findBySedeId(eq(10L), argThat(pageable ->
                pageable.getPageNumber() == 1
                        && pageable.getPageSize() == 10
                        && pageable.getSort().getOrderFor("createdAt") != null));
    }

    private User operador(Long id, Long sedeId) {
        return User.builder()
                .id(id)
                .name("Operador")
                .username("operador")
                .sedeId(sedeId)
                .build();
    }

    private Ticket ticket(Long id, Long userId, Long sedeId) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 16, 10, 0);
        return Ticket.builder()
                .id(id)
                .ticketNumber("T-" + id)
                .userId(userId)
                .sedeId(sedeId)
                .status(Ticket.TicketStatus.COMPLETED)
                .createdAt(now)
                .calledAt(now.plusMinutes(1))
                .completedAt(now.plusMinutes(5))
                .build();
    }
}
