package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.entity.yego_pro_ops.entities.MobileDriverSession;
import com.yego.backend.repository.yego_pro_ops.MobileDriverSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MobileDriverSessionService {

    private final MobileDriverSessionRepository repository;
    private final DatabaseLockService lockService;

    @Transactional
    public UUID activateOrReplace(String driverId, String deviceId, long ttlSeconds) {
        String normalizedDeviceId = normalizeDeviceId(deviceId);
        lockService.acquireAll(List.of("mobile-session:" + driverId));

        LocalDateTime now = LocalDateTime.now();
        MobileDriverSession session = repository.findById(driverId).orElse(null);
        if (session == null) {
            session = MobileDriverSession.builder().driverId(driverId).build();
        }
        session.setSessionId(UUID.randomUUID());
        session.setDeviceId(normalizedDeviceId);
        session.setIssuedAt(now);
        session.setExpiresAt(now.plusSeconds(Math.max(60, ttlSeconds)));
        session.setRevokedAt(null);
        return repository.save(session).getSessionId();
    }

    @Transactional(readOnly = true)
    public void validate(String driverId, String sessionId) {
        UUID expectedSessionId = parseSessionId(sessionId);
        MobileDriverSession session = repository.findById(driverId)
                .orElseThrow(this::invalidSession);
        if (!expectedSessionId.equals(session.getSessionId()) || !isActive(session, LocalDateTime.now())) {
            throw invalidSession();
        }
    }

    @Transactional
    public void revoke(String driverId, String sessionId) {
        UUID expectedSessionId = parseSessionId(sessionId);
        lockService.acquireAll(List.of("mobile-session:" + driverId));
        MobileDriverSession session = repository.findById(driverId)
                .orElseThrow(this::invalidSession);
        if (!expectedSessionId.equals(session.getSessionId())) {
            throw invalidSession();
        }
        session.setRevokedAt(LocalDateTime.now());
        repository.save(session);
    }

    private boolean isActive(MobileDriverSession session, LocalDateTime now) {
        return session != null
                && session.getRevokedAt() == null
                && session.getExpiresAt() != null
                && session.getExpiresAt().isAfter(now);
    }

    String normalizeDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId es requerido");
        }
        String normalized = deviceId.trim();
        if (normalized.length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId no es valido");
        }
        return normalized;
    }

    private UUID parseSessionId(String sessionId) {
        try {
            return UUID.fromString(sessionId);
        } catch (RuntimeException exception) {
            throw invalidSession();
        }
    }

    private ResponseStatusException invalidSession() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion movil invalida o vencida");
    }

}
