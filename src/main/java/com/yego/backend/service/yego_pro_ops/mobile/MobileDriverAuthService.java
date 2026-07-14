package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.config.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MobileDriverAuthService {

    private static final String TOKEN_TYPE = "mobile_driver";

    private final JwtTokenProvider jwtTokenProvider;
    private final MobileDriverSessionService mobileSessionService;

    public String requireDriverId(HttpServletRequest request) {
        return requireIdentity(request).driverId();
    }

    public MobileDriverIdentity requireIdentity(HttpServletRequest request) {
        Claims claims = parseClaims(request);
        String type = claims.get("type", String.class);
        String driverId = claims.get("driverId", String.class);
        String mobileSessionId = claims.get("mobileSessionId", String.class);

        if (!TOKEN_TYPE.equals(type)
                || driverId == null || driverId.isBlank()
                || mobileSessionId == null || mobileSessionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token movil invalido");
        }

        mobileSessionService.validate(driverId, mobileSessionId);
        return new MobileDriverIdentity(driverId, mobileSessionId);
    }

    private Claims parseClaims(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de conductor requerido");
        }

        try {
            return jwtTokenProvider.parse(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token movil invalido");
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        return token.isBlank() ? null : token;
    }

    public record MobileDriverIdentity(String driverId, String mobileSessionId) {
    }
}
