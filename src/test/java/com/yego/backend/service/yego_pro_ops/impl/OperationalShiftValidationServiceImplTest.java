package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringProperties;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalManualComparisonResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationCoverageResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalShiftSession;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalTripFact;
import com.yego.backend.repository.yego_pro_ops.OperationalShiftSessionRepository;
import com.yego.backend.repository.yego_pro_ops.OperationalTripFactRepository;
import com.yego.backend.service.yego_pro_ops.OperationalManualShiftReadAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalShiftValidationServiceImplTest {

    @Mock
    private OperationalShiftSessionRepository operationalShiftSessionRepository;
    @Mock
    private OperationalTripFactRepository operationalTripFactRepository;
    @Mock
    private OperationalManualShiftReadAdapter manualShiftReadAdapter;

    private OperationalShiftValidationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OperationalShiftValidationServiceImpl(
                operationalShiftSessionRepository,
                operationalTripFactRepository,
                manualShiftReadAdapter,
                new OperationalMonitoringProperties("America/Lima", 8, 90, 90, "complete", true));
    }

    @Test
    void matchesSameDriverWithNearbyStart() {
        arrangeBaseData(
                List.of(operationalShift("driver-1", 8, 16, "OPEN_INFERRED", false, "CAR-1", "ABC123")),
                List.of(manualShift("driver-1", 8, 15, "closed", "ABC123")),
                List.of(tripFact("driver-1", "CAR-1", "ABC123")));

        List<OperationalManualComparisonResponse> comparisons = service.getManualComparison(rangeStart(), rangeEnd(), null, null, null, null);

        assertEquals(1, comparisons.size());
        assertEquals("MATCHED", comparisons.get(0).getComparisonStatus());
        assertEquals(0L, comparisons.get(0).getStartDeltaMinutes());
    }

    @Test
    void doesNotMatchDifferentDrivers() {
        arrangeBaseData(
                List.of(operationalShift("driver-1", 8, 16, "OPEN_INFERRED", false, "CAR-1", "ABC123")),
                List.of(manualShift("driver-2", 8, 16, "closed", "ABC123")),
                List.of(tripFact("driver-1", "CAR-1", "ABC123")));

        List<OperationalManualComparisonResponse> comparisons = service.getManualComparison(rangeStart(), rangeEnd(), null, null, null, null);

        assertEquals(2, comparisons.size());
        assertEquals("AUTO_ONLY", comparisons.get(0).getComparisonStatus());
        assertEquals("MANUAL_ONLY", comparisons.get(1).getComparisonStatus());
    }

    @Test
    void avoidsDoubleMatchingAndLeavesManualOnly() {
        arrangeBaseData(
                List.of(
                        operationalShift("driver-1", 8, 10, "OPEN_INFERRED", false, "CAR-1", "ABC123"),
                        operationalShift("driver-1", 12, 18, "OPEN_INFERRED", false, "CAR-1", "ABC123")),
                List.of(manualShift("driver-1", 8, 17, "closed", "ABC123")),
                List.of(tripFact("driver-1", "CAR-1", "ABC123")));

        List<OperationalManualComparisonResponse> comparisons = service.getManualComparison(rangeStart(), rangeEnd(), null, null, null, null);

        assertEquals(2, comparisons.size());
        assertEquals(1, comparisons.stream().filter(c -> c.getManualShiftId() != null).count());
        assertEquals(1, comparisons.stream().filter(c -> "AUTO_ONLY".equals(c.getComparisonStatus())).count());
    }

    @Test
    void detectsTimeDeltaHigh() {
        arrangeBaseData(
                List.of(operationalShift("driver-1", 8, 16, "OPEN_INFERRED", false, "CAR-1", "ABC123")),
                List.of(manualShift("driver-1", 11, 19, "closed", "ABC123")),
                List.of(tripFact("driver-1", "CAR-1", "ABC123")));

        List<OperationalManualComparisonResponse> comparisons = service.getManualComparison(rangeStart(), rangeEnd(), null, null, null, null);

        assertEquals("TIME_DELTA_HIGH", comparisons.get(0).getComparisonStatus());
    }

    @Test
    void handlesMissingCloseAsInsufficientData() {
        arrangeBaseData(
                List.of(operationalShift("driver-1", 8, null, "OPEN_INFERRED", false, "CAR-1", "ABC123")),
                List.of(manualShift("driver-1", 8, 16, "active", "ABC123")),
                List.of(tripFact("driver-1", "CAR-1", "ABC123")));

        List<OperationalManualComparisonResponse> comparisons = service.getManualComparison(rangeStart(), rangeEnd(), null, null, null, null);

        assertEquals("INSUFFICIENT_DATA", comparisons.get(0).getComparisonStatus());
        assertNull(comparisons.get(0).getEndDeltaMinutes());
    }

    @Test
    void coverageCountsVehicleKeysAndNeedsReview() {
        arrangeBaseData(
                List.of(
                        operationalShift("driver-1", 8, 16, "NEEDS_REVIEW", true, null, null),
                        operationalShift("driver-1", 17, 20, "OPEN_INFERRED", false, "CAR-1", "ABC123")),
                List.of(),
                List.of(
                        tripFact("driver-1", "CAR-1", "ABC123"),
                        tripFact("driver-1", null, null)));

        OperationalValidationCoverageResponse coverage = service.getCoverage(rangeStart(), rangeEnd(), null, null);

        assertEquals(2, coverage.getOperationalTripFactCount());
        assertEquals(1, coverage.getTripFactsWithVehicleKeyCount());
        assertEquals(1, coverage.getNeedsReviewShiftCount());
        assertEquals(50.0d, coverage.getVehicleKeyCoveragePct());
        assertEquals(50.0d, coverage.getNeedsReviewShiftPct());
    }

    @Test
    void readinessCanBeNotReadyWatchPromisingAndReadyCandidate() {
        arrangeBaseData(
                List.of(
                        operationalShift("driver-1", 8, 16, "OPEN_INFERRED", false, "CAR-1", "AAA111"),
                        operationalShift("driver-2", 8, 16, "OPEN_INFERRED", false, "CAR-2", "BBB222"),
                        operationalShift("driver-3", 8, 16, "OPEN_INFERRED", false, "CAR-3", "CCC333"),
                        operationalShift("driver-4", 8, 16, "OPEN_INFERRED", false, "CAR-4", "DDD444")),
                List.of(
                        manualShift("driver-1", 8, 16, "closed", "AAA111"),
                        manualShift("driver-2", 8, 16, "closed", "BBB222"),
                        manualShift("driver-3", 8, 16, "closed", "CCC333"),
                        manualShift("driver-4", 8, 16, "closed", "DDD444")),
                List.of(
                        tripFact("driver-1", "CAR-1", "AAA111"),
                        tripFact("driver-2", "CAR-2", "BBB222"),
                        tripFact("driver-3", "CAR-3", "CCC333"),
                        tripFact("driver-4", "CAR-4", "DDD444")));

        OperationalValidationSummaryResponse summary = service.getSummary(rangeStart(), rangeEnd(), null, null);
        assertEquals("READY_CANDIDATE", summary.getManualReplacementReadiness());

        arrangeBaseData(
                List.of(
                        operationalShift("driver-1", 8, 16, "OPEN_INFERRED", false, "CAR-1", "AAA111"),
                        operationalShift("driver-2", 8, 16, "OPEN_INFERRED", false, "CAR-2", "BBB222"),
                        operationalShift("driver-3", 8, 16, "OPEN_INFERRED", false, "CAR-3", "CCC333"),
                        operationalShift("driver-4", 8, 16, "OPEN_INFERRED", false, "CAR-4", "DDD444")),
                List.of(
                        manualShift("driver-1", 8, 16, "closed", "AAA111"),
                        manualShift("driver-2", 8, 16, "closed", "BBB222"),
                        manualShift("driver-3", 8, 16, "closed", "CCC333"),
                        manualShift("driver-4", 8, 16, "closed", "DDD444")),
                List.of(
                        tripFact("driver-1", "CAR-1", "AAA111"),
                        tripFact("driver-2", "CAR-2", "BBB222"),
                        tripFact("driver-3", "CAR-3", "CCC333"),
                        tripFact("driver-4", "CAR-4", "DDD444"),
                        tripFact("driver-4", "CAR-4", "DDD444"),
                        tripFact("driver-4", "CAR-4", "DDD444"),
                        tripFact("driver-4", null, null)));
        assertEquals("PROMISING", service.getSummary(rangeStart(), rangeEnd(), null, null).getManualReplacementReadiness());

        arrangeBaseData(
                List.of(
                        operationalShift("driver-1", 8, 16, "OPEN_INFERRED", false, "CAR-1", "AAA111"),
                        operationalShift("driver-2", 8, 16, "OPEN_INFERRED", false, "CAR-2", "BBB222"),
                        operationalShift("driver-3", 8, 16, "OPEN_INFERRED", false, "CAR-3", "CCC333"),
                        operationalShift("driver-4", 8, 16, "OPEN_INFERRED", false, "CAR-4", "DDD444"),
                        operationalShift("driver-5", 8, 16, "OPEN_INFERRED", false, "CAR-5", "EEE555")),
                List.of(
                        manualShift("driver-1", 8, 16, "closed", "AAA111"),
                        manualShift("driver-2", 8, 16, "closed", "BBB222"),
                        manualShift("driver-3", 8, 16, "closed", "CCC333"),
                        manualShift("driver-4", 8, 16, "closed", "DDD444"),
                        manualShift("driver-5", 8, 16, "closed", "EEE555")),
                List.of(
                        tripFact("driver-1", "CAR-1", "AAA111"),
                        tripFact("driver-2", "CAR-2", "BBB222"),
                        tripFact("driver-3", "CAR-3", "CCC333"),
                        tripFact("driver-4", "CAR-4", "DDD444"),
                        tripFact("driver-5", null, null)));
        assertEquals("WATCH", service.getSummary(rangeStart(), rangeEnd(), null, null).getManualReplacementReadiness());

        arrangeBaseData(
                List.of(
                        operationalShift("driver-1", 8, 16, "NEEDS_REVIEW", true, null, null),
                        operationalShift("driver-2", 12, 18, "OPEN_INFERRED", false, null, null)),
                List.of(
                        manualShift("driver-1", 8, 16, "closed", "AAA111"),
                        manualShift("driver-2", 8, 16, "closed", "BBB222")),
                List.of(
                        tripFact("driver-1", null, null),
                        tripFact("driver-2", null, null),
                        tripFact("driver-2", null, null)));
        assertEquals("NOT_READY", service.getSummary(rangeStart(), rangeEnd(), null, null).getManualReplacementReadiness());
    }

    @Test
    void validationServiceReadsOnlyPersistedDataAndDoesNotCallYango() {
        arrangeBaseData(List.of(), List.of(), List.of());

        service.getCoverage(rangeStart(), rangeEnd(), null, null);

        verify(operationalTripFactRepository).findForValidation(any(), any(), any(), any());
        verify(operationalShiftSessionRepository).findForValidation(any(), any(), any(), any());
        verify(manualShiftReadAdapter).findManualShifts(any(), any(), any(), any());
        verifyNoMoreInteractions(operationalTripFactRepository, operationalShiftSessionRepository, manualShiftReadAdapter);
    }

    private void arrangeBaseData(
            List<OperationalShiftSession> operationalShifts,
            List<OperationalManualShiftReadAdapter.ManualShiftSnapshot> manualShifts,
            List<OperationalTripFact> tripFacts) {
        when(operationalShiftSessionRepository.findForValidation(any(), any(), any(), any())).thenReturn(operationalShifts);
        when(manualShiftReadAdapter.findManualShifts(any(), any(), any(), any())).thenReturn(manualShifts);
        when(operationalTripFactRepository.findForValidation(any(), any(), any(), any())).thenReturn(tripFacts);
    }

    private OperationalShiftSession operationalShift(
            String driverId,
            Integer startHour,
            Integer endHour,
            String state,
            boolean needsReview,
            String vehicleKey,
            String plateNormalized) {
        return OperationalShiftSession.builder()
                .id(UUID.randomUUID())
                .driverId(driverId)
                .openedAt(LocalDateTime.of(2026, 6, 25, startHour, 0))
                .closedAt(endHour == null ? null : LocalDateTime.of(2026, 6, 25, endHour, 0))
                .state(state)
                .vehicleKey(vehicleKey)
                .vehiclePlateNormalized(plateNormalized)
                .needsReview(needsReview)
                .reviewReason(needsReview ? "MISSING_VEHICLE_KEY" : null)
                .confidenceLevel("HIGH")
                .build();
    }

    private OperationalManualShiftReadAdapter.ManualShiftSnapshot manualShift(
            String driverId,
            int startHour,
            Integer endHour,
            String status,
            String plateNormalized) {
        return new OperationalManualShiftReadAdapter.ManualShiftSnapshot(
                UUID.randomUUID(),
                driverId,
                LocalDateTime.of(2026, 6, 25, startHour, 0),
                endHour == null ? null : LocalDateTime.of(2026, 6, 25, endHour, 0),
                status,
                plateNormalized);
    }

    private OperationalTripFact tripFact(String driverId, String vehicleKey, String plateNormalized) {
        return OperationalTripFact.builder()
                .id(UUID.randomUUID())
                .driverId(driverId)
                .vehicleKey(vehicleKey)
                .vehiclePlateNormalized(plateNormalized)
                .bookedAt(rangeStart())
                .build();
    }

    private LocalDateTime rangeStart() {
        return LocalDateTime.of(2026, 6, 25, 0, 0);
    }

    private LocalDateTime rangeEnd() {
        return LocalDateTime.of(2026, 6, 25, 23, 59);
    }
}
