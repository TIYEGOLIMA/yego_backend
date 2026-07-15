package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.entity.yego_pro_ops.entities.MobileDriverSession;
import com.yego.backend.repository.yego_pro_ops.MobileDriverSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileDriverSessionServiceTest {

    @Mock
    private MobileDriverSessionRepository repository;

    @Mock
    private DatabaseLockService lockService;

    @InjectMocks
    private MobileDriverSessionService service;

    @Test
    void replacesActiveSessionAfterOtpVerificationOnAnotherDevice() {
        MobileDriverSession active = activeSession("driver-1", "device-a");
        UUID previousSessionId = active.getSessionId();
        when(repository.findById("driver-1")).thenReturn(Optional.of(active));
        when(repository.save(any(MobileDriverSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID currentSessionId = service.activateOrReplace("driver-1", "device-b", 600);

        assertNotEquals(previousSessionId, currentSessionId);
        assertEquals(currentSessionId, active.getSessionId());
        assertEquals("device-b", active.getDeviceId());
        assertNull(active.getRevokedAt());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.validate("driver-1", previousSessionId.toString())
        );
        assertEquals(401, error.getStatusCode().value());
    }

    @Test
    void rotatesSessionTokenWhenSameDeviceLogsInAgain() {
        MobileDriverSession active = activeSession("driver-1", "device-a");
        UUID previousSessionId = active.getSessionId();
        when(repository.findById("driver-1")).thenReturn(Optional.of(active));
        when(repository.save(any(MobileDriverSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID currentSessionId = service.activateOrReplace("driver-1", "device-a", 600);

        assertNotEquals(previousSessionId, currentSessionId);
    }

    private MobileDriverSession activeSession(String driverId, String deviceId) {
        return MobileDriverSession.builder()
                .driverId(driverId)
                .deviceId(deviceId)
                .sessionId(UUID.randomUUID())
                .issuedAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
    }
}
