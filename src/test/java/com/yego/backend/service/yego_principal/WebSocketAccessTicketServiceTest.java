package com.yego.backend.service.yego_principal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketAccessTicketServiceTest {

    @Test
    void ticketEsDeUnSoloUsoYConservaElAlcanceDelDispositivo() {
        WebSocketAccessTicketService service = new WebSocketAccessTicketService(30);
        Claims claims = claims(Instant.now().plusSeconds(604_800), Map.of(
                "dispositivoId", 12L,
                "tipo", "TABLET",
                "sedeId", 3L,
                "moduleId", 8L,
                "tokenVersion", 4));

        var issued = service.issue(claims);
        var principal = service.consume(issued.ticket());

        assertThat(issued.expiresAt()).isAfter(Instant.now());
        assertThat(principal.isDevice()).isTrue();
        assertThat(principal.dispositivoId()).isEqualTo(12L);
        assertThat(principal.sedeId()).isEqualTo(3L);
        assertThat(principal.moduleId()).isEqualTo(8L);
        assertThat(principal.tokenVersion()).isEqualTo(4);
        assertThatThrownBy(() -> service.consume(issued.ticket()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inválido o expirado");
    }

    @Test
    void noEmiteTicketParaJwtVencido() {
        WebSocketAccessTicketService service = new WebSocketAccessTicketService(30);
        Claims expired = claims(Instant.now().minusSeconds(1), Map.of("userId", 5L));

        assertThatThrownBy(() -> service.issue(expired))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("JWT expirado");
    }

    private static Claims claims(Instant expiresAt, Map<String, Object> values) {
        return Jwts.claims()
                .add(values)
                .expiration(Date.from(expiresAt))
                .build();
    }
}
