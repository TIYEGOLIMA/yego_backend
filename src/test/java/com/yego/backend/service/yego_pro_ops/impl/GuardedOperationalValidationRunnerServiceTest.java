package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringRunnerProperties;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalTripFact;
import com.yego.backend.service.yego_pro_ops.OperationalShiftInferenceService;
import com.yego.backend.service.yego_pro_ops.OperationalShiftValidationService;
import com.yego.backend.service.yego_pro_ops.OperationalTripFactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardedOperationalValidationRunnerServiceTest {

    @Mock
    private OperationalEnvironmentGuard environmentGuard;

    @Mock
    private OperationalMigrationDiagnosticsService migrationDiagnosticsService;

    @Mock
    private OperationalValidationSampleSelector sampleSelector;

    @Mock
    private OperationalTripFactService tripFactService;

    @Mock
    private OperationalShiftInferenceService shiftInferenceService;

    @Mock
    private OperationalShiftValidationService validationService;

    private OperationalMonitoringRunnerProperties properties;
    private GuardedOperationalValidationRunnerService service;

    @BeforeEach
    void setUp() {
        properties = new OperationalMonitoringRunnerProperties();
        properties.setEnabled(true);
        properties.setEnvironment("staging");
        properties.setConfirmWritesToOperationalTables(true);
        properties.setDateFrom(LocalDate.of(2026, 6, 23));
        properties.setDateTo(LocalDate.of(2026, 6, 23));
        properties.setDriverIds(List.of("driver-1", "driver-2"));
        service = new GuardedOperationalValidationRunnerService(
                properties,
                environmentGuard,
                migrationDiagnosticsService,
                sampleSelector,
                tripFactService,
                shiftInferenceService,
                validationService);
    }

    @Test
    void failsClosedWhenOperationalMigrationIsMissing() {
        when(migrationDiagnosticsService.inspect()).thenReturn(new OperationalMigrationDiagnosticsService.MigrationDiagnostics(
                true, false, true, false, "Manual SQL", false, "N/A", true, "validate", true));

        assertThrows(IllegalStateException.class, () -> service.runOnce());

        verify(tripFactService, never()).importFromDriverOrders(any(), any(), any());
        verify(shiftInferenceService, never()).reprocessRange(any(), any(), any(), any());
        verify(validationService, never()).getSummary(any(), any(), any(), any());
    }

    @Test
    void runnerCallsOnlyOperationalServicesAndAggregatesResults() {
        when(migrationDiagnosticsService.inspect()).thenReturn(new OperationalMigrationDiagnosticsService.MigrationDiagnostics(
                true, true, true, true, "Manual SQL", false, "N/A", true, "validate", true));
        when(tripFactService.importFromDriverOrders(any(), any(), any()))
                .thenReturn(List.of(OperationalTripFact.builder().externalTripId("trip-1").driverId("driver-1").build()));
        when(shiftInferenceService.reprocessRange(any(), any(), any(), any()))
                .thenReturn(new OperationalShiftInferenceService.ReprocessResult(3, 1, 2));
        when(validationService.getSummary(any(), any(), any(), any()))
                .thenReturn(OperationalValidationSummaryResponse.builder()
                        .needsReviewShiftCount(1L)
                        .autoClosedByNextDriverCount(2L)
                        .staleCandidateCount(3L)
                        .manualReplacementReadiness("WATCH")
                        .build());

        GuardedOperationalValidationRunnerService.ExecutionReport report = service.runOnce();

        assertEquals(List.of("driver-1", "driver-2"), report.requestedDriverIds());
        assertEquals(List.of("driver-1", "driver-2"), report.successfulDriverIds());
        assertEquals(2, report.importedTripFacts());
        assertEquals(6, report.tripFactsConsidered());
        assertEquals(2, report.sessionsCreated());
        assertEquals(4, report.eventsCreated());
        assertEquals(2L, report.needsReviewShiftCount());
        assertEquals(4L, report.autoClosedByNextDriverCount());
        assertEquals(6L, report.staleCandidateCount());
        assertEquals("WATCH", report.summaryReadinessByDriver().get("driver-1"));

        verify(tripFactService).importFromDriverOrders("driver-1", "2026-06-23", "2026-06-23");
        verify(tripFactService).importFromDriverOrders("driver-2", "2026-06-23", "2026-06-23");
        verify(shiftInferenceService).reprocessRange(
                eq(LocalDateTime.of(2026, 6, 23, 0, 0)),
                eq(LocalDateTime.of(2026, 6, 23, 23, 59, 59, 999999999)),
                eq("driver-1"),
                eq(null));
        verify(shiftInferenceService).reprocessRange(
                eq(LocalDateTime.of(2026, 6, 23, 0, 0)),
                eq(LocalDateTime.of(2026, 6, 23, 23, 59, 59, 999999999)),
                eq("driver-2"),
                eq(null));
    }
}
