package com.yego.backend.config;

import com.yego.backend.entity.yego_principal.api.response.UserResponseDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final int HS512_MINIMUM_KEY_BYTES = 64;
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 30;

    private final SecretKey signingKey;
    private final JwtParser parser;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:604800}") long expirationSeconds) {
        byte[] keyBytes = secret == null
                ? new byte[0]
                : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < HS512_MINIMUM_KEY_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret debe contener al menos 64 bytes para firmar con HS512");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalStateException("jwt.expiration debe ser mayor que cero");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .clockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                .build();
    }

    public Claims parse(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token JWT requerido");
        }
        return parser.parseSignedClaims(token).getPayload();
    }

    public String generate(UserResponseDto user) {
        if (user == null || user.getId() == null || user.getUsername() == null) {
            throw new IllegalArgumentException("Usuario inválido para generar JWT");
        }

        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRoleName())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(expirationSeconds)))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    public String generate(String subject, Map<String, ?> claims, long tokenExpirationSeconds) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("El subject JWT es obligatorio");
        }
        if (tokenExpirationSeconds <= 0) {
            throw new IllegalArgumentException("La expiración JWT debe ser mayor que cero");
        }

        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .claims(claims == null ? Map.of() : claims)
                .subject(subject)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(tokenExpirationSeconds)))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }
}
