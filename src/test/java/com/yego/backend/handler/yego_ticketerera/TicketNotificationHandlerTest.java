package com.yego.backend.handler.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.TicketWithCategoryResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Ticket;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import com.yego.backend.service.yego_principal.FilteredWebSocketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketNotificationHandlerTest {

    @Mock
    private FilteredWebSocketService filteredWebSocketService;
    @Mock
    private OptionRepository optionRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private TicketNotificationHandler handler;

    @Test
    void ticketCreadoSePublicaUnaVezEnLaSedeCorrecta() {
        Ticket ticket = Ticket.builder()
                .id(51L)
                .ticketNumber("L-051")
                .status(Ticket.TicketStatus.WAITING)
                .priority(1)
                .sedeId(10L)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        handler.enviarNuevoTicket(ticket);

        verify(filteredWebSocketService, times(1)).convertAndSend(
                eq("/topic/ticketera/sedes/10/tickets"),
                argThat(payload -> esEvento(payload, "TICKET_CREATED", 10L, null, 51L)));
        verify(filteredWebSocketService, never()).convertAndSend(
                eq("/topic/ticketera/sedes/20/tickets"), any());
    }

    @Test
    void ticketCompletadoEnviaRatingUnaVezSoloAlModuloVinculado() {
        TicketWithCategoryResponse ticket = TicketWithCategoryResponse.builder()
                .id(88L)
                .ticketNumber("M8-088")
                .status("COMPLETED")
                .priority(1)
                .sedeId(3L)
                .moduleId(8L)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .completedAt(LocalDateTime.of(2026, 1, 1, 10, 5))
                .build();

        handler.enviarTicketCompletado(ticket);

        String ratingTopic = "/topic/ticketera/sedes/3/modules/8/rating";
        verify(filteredWebSocketService, times(1)).convertAndSend(
                eq(ratingTopic),
                argThat(payload -> esEvento(payload, "TICKET_COMPLETED", 3L, 8L, 88L)));
        verify(filteredWebSocketService, times(1)).convertAndSend(
                argThat(topic -> topic.startsWith("/topic/ticketera/sedes/") && topic.endsWith("/rating")),
                any());
        verify(filteredWebSocketService, never()).convertAndSend(
                eq("/topic/ticketera/sedes/3/modules/9/rating"), any());
        verify(filteredWebSocketService, never()).convertAndSend(
                eq("/topic/ticketera/sedes/4/modules/8/rating"), any());
    }

    private static boolean esEvento(
            Object payload,
            String type,
            Long sedeId,
            Long moduleId,
            Long ticketId) {
        if (!(payload instanceof Map<?, ?> event)) return false;
        Object dataValue = event.get("data");

        Long dataTicketId = null;
        if (dataValue instanceof TicketWithCategoryResponse response) {
            dataTicketId = response.getId();
        } else if (dataValue instanceof com.yego.backend.entity.yego_ticketerera.api.response.TicketWebSocketResponse response) {
            dataTicketId = response.getId();
        }

        assertThat(event.get("eventId")).isInstanceOf(String.class);
        assertThat(event.get("occurredAt")).isInstanceOf(String.class);
        return type.equals(event.get("type"))
                && sedeId.equals(event.get("sedeId"))
                && java.util.Objects.equals(moduleId, event.get("moduleId"))
                && ticketId.equals(dataTicketId);
    }
}
