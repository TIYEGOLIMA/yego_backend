package com.yego.backend.service.yego_marketing_mensajes.sender;

import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MarketingFleetSenderTest {

    @Test
    void reutilizaIdempotencyKeyPersistida() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        MarketingDispatchTracker tracker = mock(MarketingDispatchTracker.class);
        UUID idempotencyKey = UUID.randomUUID();
        MarketingDispatchTracker.Claim claim =
                new MarketingDispatchTracker.Claim(2L, idempotencyKey, 1);
        Instant scheduledFor = Instant.parse("2026-07-13T15:00:00Z");
        when(tracker.reclamar(
                10L, MarketingDispatchTracker.CHANNEL_FLEET, "park-1", scheduledFor))
                .thenReturn(Optional.of(claim));

        MarketingFleetSender sender = new MarketingFleetSender(
                restTemplate,
                tracker,
                "https://fleet.test/mailings",
                "session=value; park_id=old",
                0);

        server.expect(once(), requestTo("https://fleet.test/mailings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-park-id", "park-1"))
                .andExpect(header("X-Idempotency-Token", idempotencyKey.toString()))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        MarketingMensaje mensaje = new MarketingMensaje();
        mensaje.setId(10L);
        mensaje.setTitulo("Campaña");
        mensaje.setMensaje("Mensaje");

        MarketingDeliveryResult result = sender.enviar(
                mensaje, List.of("park-1"), scheduledFor);

        assertThat(result.enviados()).isEqualTo(1);
        verify(tracker).marcarEnviado(claim, 200, null);
        server.verify();
    }
}
