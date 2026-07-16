package com.yego.backend.service.yego_principal;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketAccessTicketService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, StoredTicket> tickets = new ConcurrentHashMap<>();
    private final long ticketTtlSeconds;

    public WebSocketAccessTicketService(
            @Value("${websocket.access-ticket-ttl-seconds:30}") long ticketTtlSeconds) {
        this.ticketTtlSeconds = ticketTtlSeconds;
    }

    public IssuedTicket issue(Claims claims) {
        if (claims == null || claims.getExpiration() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT requerido para WebSocket");
        }

        Instant now = Instant.now();
        Instant tokenExpiresAt = claims.getExpiration().toInstant();
        if (!tokenExpiresAt.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT expirado");
        }

        cleanupExpired(now);
        String ticket = randomTicket();
        Instant expiresAt = now.plusSeconds(ticketTtlSeconds).isBefore(tokenExpiresAt)
                ? now.plusSeconds(ticketTtlSeconds)
                : tokenExpiresAt;
        TicketPrincipal principal = new TicketPrincipal(
                claimLong(claims, "userId"),
                claims.get("username", String.class),
                claims.get("role", String.class),
                claimLong(claims, "dispositivoId"),
                claims.get("tipo", String.class),
                claimLong(claims, "sedeId"),
                claimLong(claims, "moduleId"),
                claimInteger(claims, "tokenVersion"),
                tokenExpiresAt
        );
        tickets.put(ticket, new StoredTicket(principal, expiresAt));
        return new IssuedTicket(ticket, expiresAt);
    }

    public TicketPrincipal consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ticket WebSocket requerido");
        }
        StoredTicket stored = tickets.remove(ticket);
        Instant now = Instant.now();
        if (stored == null
                || !stored.expiresAt().isAfter(now)
                || !stored.principal().tokenExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ticket WebSocket inválido o expirado");
        }
        return stored.principal();
    }

    private void cleanupExpired(Instant now) {
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private static String randomTicket() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static Long claimLong(Claims claims, String name) {
        Object value = claims.get(name);
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Integer claimInteger(Claims claims, String name) {
        Object value = claims.get(name);
        return value instanceof Number number ? number.intValue() : null;
    }

    private record StoredTicket(TicketPrincipal principal, Instant expiresAt) {}

    public record IssuedTicket(String ticket, Instant expiresAt) {}

    public record TicketPrincipal(
            Long userId,
            String username,
            String role,
            Long dispositivoId,
            String tipoDispositivo,
            Long sedeId,
            Long moduleId,
            Integer tokenVersion,
            Instant tokenExpiresAt) {

        public boolean isDevice() {
            return dispositivoId != null;
        }
    }
}
