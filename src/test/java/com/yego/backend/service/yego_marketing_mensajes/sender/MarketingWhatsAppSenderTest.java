package com.yego.backend.service.yego_marketing_mensajes.sender;

import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingWhatsAppSenderTest {

    @Test
    void enviaTextoConContratoEvolutionGo() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        MarketingDispatchTracker tracker = mock(MarketingDispatchTracker.class);
        MarketingDispatchTracker.Claim claim =
                new MarketingDispatchTracker.Claim(1L, UUID.randomUUID(), 1);
        Instant scheduledFor = Instant.parse("2026-07-13T15:00:00Z");
        when(tracker.reclamar(
                10L, MarketingDispatchTracker.CHANNEL_WHATSAPP, "grupo-1", scheduledFor))
                .thenReturn(Optional.of(claim));
        MarketingWhatsAppSender sender = new MarketingWhatsAppSender(
                restTemplate,
                tracker,
                "https://go.yego.pro/",
                "token-marketing",
                "Yego_Marketing",
                "/group/list",
                1200);

        server.expect(once(), requestTo("https://go.yego.pro/send/text"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "token-marketing"))
                .andExpect(header("Authorization", "Bearer token-marketing"))
                .andExpect(jsonPath("$.number").value("grupo-1"))
                .andExpect(jsonPath("$.text").value("Hola"))
                .andExpect(jsonPath("$.delay").value(1200))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        MarketingMensaje mensaje = new MarketingMensaje();
        mensaje.setId(10L);
        mensaje.setMensaje("Hola");
        mensaje.setTipo("ninguna");

        MarketingDeliveryResult result = sender.enviar(
                mensaje, List.of("grupo-1"), scheduledFor);

        assertThat(result.enviados()).isEqualTo(1);
        assertThat(result.fallidos()).isZero();
        verify(tracker).marcarEnviado(claim, 200, null);
        server.verify();
    }

    @Test
    void adaptaRespuestaRealDeGruposAlModeloInterno() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        MarketingWhatsAppSender sender = new MarketingWhatsAppSender(
                restTemplate,
                mock(MarketingDispatchTracker.class),
                "https://go.yego.pro",
                "token-marketing",
                "Yego_Marketing",
                "/group/list",
                0);

        server.expect(once(), requestTo("https://go.yego.pro/group/list"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {
                          "message": "success",
                          "data": [{
                            "JID": "grupo-1@g.us",
                            "Name": "Conductores",
                            "OwnerJID": "owner@s.whatsapp.net",
                            "ParticipantCount": 25,
                            "IsLocked": false,
                            "IsAnnounce": true,
                            "IsParent": false,
                            "IsDefaultSubGroup": false
                          }]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        List<Map<String, Object>> groups = sender.obtenerGrupos();

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0))
                .containsEntry("id", "grupo-1@g.us")
                .containsEntry("subject", "Conductores")
                .containsEntry("size", 25)
                .containsEntry("announce", true);
        server.verify();
    }
}
