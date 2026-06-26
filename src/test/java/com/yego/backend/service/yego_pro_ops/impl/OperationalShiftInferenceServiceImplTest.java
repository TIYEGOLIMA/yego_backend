package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringProperties;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalShiftEvent;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalShiftSession;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalTripFact;
import com.yego.backend.repository.yego_pro_ops.OperationalShiftEventRepository;
import com.yego.backend.repository.yego_pro_ops.OperationalShiftSessionRepository;
import com.yego.backend.repository.yego_pro_ops.OperationalTripFactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalShiftInferenceServiceImplTest {

    @Mock
    private OperationalTripFactRepository tripFactRepository;
    @Mock
    private OperationalShiftSessionRepository shiftSessionRepository;
    @Mock
    private OperationalShiftEventRepository shiftEventRepository;

    private OperationalShiftInferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OperationalShiftInferenceServiceImpl(
                tripFactRepository,
                shiftSessionRepository,
                shiftEventRepository,
                new OperationalMonitoringProperties("America/Lima", 8, "complete", true));
    }

    @Test
    void firstTripOpensAndSecondTripSameDriverContinuesSession() {
        when(tripFactRepository.findForInference(any(), any(), isNull(), isNull())).thenReturn(List.of(
                fact("trip-1", "driver-1", "CAR-1", "complete", LocalDateTime.of(2026, 6, 25, 8, 0), null),
                fact("trip-2", "driver-1", "CAR-1", "complete", LocalDateTime.of(2026, 6, 25, 9, 0), null)
        ));
        when(shiftSessionRepository.findForReprocess(any(), any(), isNull(), isNull())).thenReturn(List.of());
        when(shiftSessionRepository.saveAll(any())).thenAnswer(invocation -> withIds(invocation.getArgument(0)));
        when(shiftEventRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.reprocessRange(LocalDateTime.of(2026, 6, 25, 0, 0), LocalDateTime.of(2026, 6, 25, 23, 59), null, null);

        ArgumentCaptor<List<OperationalShiftSession>> sessionCaptor = ArgumentCaptor.forClass(List.class);
        verify(shiftSessionRepository).saveAll(sessionCaptor.capture());
        OperationalShiftSession session = sessionCaptor.getValue().get(0);
        assertEquals(OperationalMonitoringConstants.STATE_STALE_CANDIDATE, session.getState());
        assertEquals(2, session.getTripCount());
        assertEquals("trip-2", session.getLastTripExternalId());
    }

    @Test
    void differentDriverSameVehicleClosesPreviousAndOpensNewSession() {
        when(tripFactRepository.findForInference(any(), any(), isNull(), isNull())).thenReturn(List.of(
                fact("trip-1", "driver-1", "CAR-1", "complete", LocalDateTime.of(2026, 6, 25, 8, 0), null),
                fact("trip-2", "driver-2", "CAR-1", "complete", LocalDateTime.of(2026, 6, 25, 9, 0), null)
        ));
        when(shiftSessionRepository.findForReprocess(any(), any(), isNull(), isNull())).thenReturn(List.of());
        when(shiftSessionRepository.saveAll(any())).thenAnswer(invocation -> withIds(invocation.getArgument(0)));
        when(shiftEventRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.reprocessRange(LocalDateTime.of(2026, 6, 25, 0, 0), LocalDateTime.of(2026, 6, 25, 12, 0), null, null);

        ArgumentCaptor<List<OperationalShiftSession>> sessionCaptor = ArgumentCaptor.forClass(List.class);
        verify(shiftSessionRepository).saveAll(sessionCaptor.capture());
        List<OperationalShiftSession> sessions = sessionCaptor.getValue();
        assertEquals(2, sessions.size());
        assertEquals(OperationalMonitoringConstants.STATE_AUTO_CLOSED_BY_NEXT_DRIVER, sessions.get(0).getState());
        assertEquals(LocalDateTime.of(2026, 6, 25, 9, 0), sessions.get(0).getClosedAt());
        assertEquals(OperationalMonitoringConstants.STATE_OPEN_INFERRED, sessions.get(1).getState());
    }

    @Test
    void endedAtFallbackLowersConfidenceAndCancelledTripsAreIgnored() {
        when(tripFactRepository.findForInference(any(), any(), isNull(), isNull())).thenReturn(List.of(
                fact("trip-cancelled", "driver-1", "CAR-1", "cancelled", LocalDateTime.of(2026, 6, 25, 7, 0), null),
                fact("trip-fallback", "driver-1", "CAR-1", "complete", null, LocalDateTime.of(2026, 6, 25, 8, 30))
        ));
        when(shiftSessionRepository.findForReprocess(any(), any(), isNull(), isNull())).thenReturn(List.of());
        when(shiftSessionRepository.saveAll(any())).thenAnswer(invocation -> withIds(invocation.getArgument(0)));
        when(shiftEventRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.reprocessRange(LocalDateTime.of(2026, 6, 25, 0, 0), LocalDateTime.of(2026, 6, 25, 10, 0), null, null);

        ArgumentCaptor<List<OperationalShiftSession>> sessionCaptor = ArgumentCaptor.forClass(List.class);
        verify(shiftSessionRepository).saveAll(sessionCaptor.capture());
        OperationalShiftSession session = sessionCaptor.getValue().get(0);
        assertEquals(1, session.getTripCount());
        assertEquals(OperationalMonitoringConstants.CONFIDENCE_LOW, session.getConfidenceLevel());
        assertTrue(session.getNeedsReview());
    }

    @Test
    void reprocessSameRangeDoesNotDuplicateSessions() {
        List<OperationalTripFact> facts = List.of(
                fact("trip-1", "driver-1", "CAR-1", "complete", LocalDateTime.of(2026, 6, 25, 23, 50), null),
                fact("trip-2", "driver-1", "CAR-1", "complete", LocalDateTime.of(2026, 6, 26, 0, 10), null)
        );
        when(tripFactRepository.findForInference(any(), any(), isNull(), isNull())).thenReturn(facts);

        AtomicReference<List<OperationalShiftSession>> storedSessions = new AtomicReference<>(new ArrayList<>());
        when(shiftSessionRepository.findForReprocess(any(), any(), isNull(), isNull())).thenAnswer(invocation -> new ArrayList<>(storedSessions.get()));
        when(shiftSessionRepository.saveAll(any())).thenAnswer(invocation -> {
            List<OperationalShiftSession> sessions = withIds(invocation.getArgument(0));
            storedSessions.set(new ArrayList<>(sessions));
            return sessions;
        });
        doAnswer(invocation -> {
            storedSessions.set(new ArrayList<>());
            return null;
        }).when(shiftSessionRepository).deleteAll(anyCollection());
        when(shiftEventRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.reprocessRange(LocalDateTime.of(2026, 6, 25, 0, 0), LocalDateTime.of(2026, 6, 26, 8, 0), null, null);
        service.reprocessRange(LocalDateTime.of(2026, 6, 25, 0, 0), LocalDateTime.of(2026, 6, 26, 8, 0), null, null);

        assertEquals(1, storedSessions.get().size());
        assertEquals(2, storedSessions.get().get(0).getTripCount());
    }

    private OperationalTripFact fact(
            String externalTripId,
            String driverId,
            String vehicleKey,
            String status,
            LocalDateTime bookedAt,
            LocalDateTime endedAt) {
        return OperationalTripFact.builder()
                .externalTripId(externalTripId)
                .driverId(driverId)
                .vehicleKey(vehicleKey)
                .vehicleKeySource(OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_YANGO_CAR_ID)
                .vehicleId(vehicleKey)
                .vehiclePlateNormalized(vehicleKey)
                .tripStatus(status)
                .bookedAt(bookedAt)
                .endedAt(endedAt)
                .observedAt((bookedAt != null ? bookedAt : endedAt).plusMinutes(5))
                .build();
    }

    private List<OperationalShiftSession> withIds(List<OperationalShiftSession> sessions) {
        for (OperationalShiftSession session : sessions) {
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
        }
        return sessions;
    }
}
