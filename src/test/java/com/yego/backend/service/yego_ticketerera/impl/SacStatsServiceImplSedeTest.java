package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Sede;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.repository.yego_ticketerera.QueueAgentRepository;
import com.yego.backend.repository.yego_ticketerera.QueueRatingRepository;
import com.yego.backend.repository.yego_ticketerera.SedeRepository;
import com.yego.backend.repository.yego_ticketerera.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SacStatsServiceImplSedeTest {

    @Mock private UserRepository userRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private QueueRatingRepository queueRatingRepository;
    @Mock private QueueAgentRepository queueAgentRepository;
    @Mock private SedeRepository sedeRepository;

    private SacStatsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SacStatsServiceImpl(
                userRepository, ticketRepository, queueRatingRepository, queueAgentRepository, sedeRepository);
        when(sedeRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(
                Sede.builder().id(10L).name("Lima").build(),
                Sede.builder().id(20L).name("Callao").build()));
    }

    @Test
    void filtraTicketsYCalificacionesRecientesPorSedeAunqueElOperadorHayaCambiado() {
        User operador = operador(1L, 20L);
        Ticket ticketLima = ticket(101L, 1L, 10L);

        when(userRepository.findByRoleName("SAC")).thenReturn(List.of(operador));
        when(ticketRepository.countBySedeId(10L)).thenReturn(1L);
        when(queueRatingRepository.countBySedeId(10L)).thenReturn(0L);
        when(queueRatingRepository.getAverageRatingBySedeId(10L)).thenReturn(0.0);
        when(ticketRepository.findByUserIdInAndSedeId(List.of(1L), 10L)).thenReturn(List.of(ticketLima));
        when(queueRatingRepository.findRecentRatingsBySedeId(any(Pageable.class), eq(10L)))
                .thenReturn(List.of());

        SacStatsResponse response = service.obtenerTodasLasEstadisticas(null, null, 10L);

        assertThat(response.getTotalTickets()).isEqualTo(1);
        assertThat(response.getSacPerformance()).singleElement().satisfies(performance -> {
            assertThat(performance.getSedeId()).isEqualTo(10L);
            assertThat(performance.getSedeName()).isEqualTo("Lima");
            assertThat(performance.getTotalTickets()).isEqualTo(1);
        });
        assertThat(response.getHourlyBySede()).extracting(SacStatsResponse.HourlyBySede::getSedeId)
                .containsExactly(10L);
        verify(queueRatingRepository).findRecentRatingsBySedeId(any(Pageable.class), eq(10L));
        verify(queueRatingRepository, never()).findRecentRatings(any(Pageable.class));
    }

    @Test
    void separaElHistorialDelMismoOperadorPorLaSedeRealDeCadaTicket() {
        User operador = operador(1L, 20L);
        when(userRepository.findByRoleName("SAC")).thenReturn(List.of(operador));
        when(ticketRepository.count()).thenReturn(2L);
        when(queueRatingRepository.count()).thenReturn(0L);
        when(queueRatingRepository.getAverageRating()).thenReturn(0.0);
        when(ticketRepository.findByUserIdIn(List.of(1L))).thenReturn(List.of(
                ticket(101L, 1L, 10L), ticket(202L, 1L, 20L)));
        when(queueRatingRepository.findRecentRatings(any(Pageable.class))).thenReturn(List.of());

        SacStatsResponse response = service.obtenerTodasLasEstadisticas(null, null, null);

        assertThat(response.getSacPerformance())
                .extracting(SacStatsResponse.SacPerformanceResponse::getSedeId)
                .containsExactly(10L, 20L);
        assertThat(response.getSacPerformance())
                .extracting(SacStatsResponse.SacPerformanceResponse::getTotalTickets)
                .containsExactly(1, 1);
    }

    @Test
    void combinaFiltroDeFechaYSedeParaCalificacionesRecientes() {
        User operador = operador(1L, 10L);
        when(userRepository.findByRoleName("SAC")).thenReturn(List.of(operador));
        when(ticketRepository.countBySedeIdAndCreatedAtBetween(eq(10L), any(), any())).thenReturn(0L);
        when(queueRatingRepository.countBySedeIdAndCreatedAtBetween(eq(10L), any(), any())).thenReturn(0L);
        when(queueRatingRepository.getAverageRatingBySedeIdAndDateRange(eq(10L), any(), any())).thenReturn(0.0);
        when(ticketRepository.findByUserIdInAndSedeIdAndCreatedAtBetween(eq(List.of(1L)), eq(10L), any(), any()))
                .thenReturn(List.of());
        when(queueRatingRepository.findRecentRatingsBySedeIdAndDateRange(
                any(Pageable.class), eq(10L), any(), any())).thenReturn(List.of());

        service.obtenerTodasLasEstadisticas("2026-07-01", "2026-07-16", 10L);

        verify(queueRatingRepository).findRecentRatingsBySedeIdAndDateRange(
                any(Pageable.class), eq(10L), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(queueRatingRepository, never()).findRecentRatingsByDateRange(any(), any(), any());
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
