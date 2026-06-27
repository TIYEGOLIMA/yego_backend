package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringRunnerProperties;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.repository.yego_pro_ops.OperationalValidationShiftSampleReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalValidationSampleSelectorTest {

    @Mock
    private OperationalValidationShiftSampleReadRepository repository;

    private OperationalValidationSampleSelector selector;

    @BeforeEach
    void setUp() {
        selector = new OperationalValidationSampleSelector(repository);
    }

    @Test
    void selectorLimitsUniqueDriversToSafeDefaultCount() {
        OperationalMonitoringRunnerProperties properties = new OperationalMonitoringRunnerProperties();
        properties.setDateFrom(LocalDate.of(2026, 6, 23));
        properties.setDefaultDriverCount(2);

        when(repository.findForSampleSelection(
                LocalDateTime.of(2026, 6, 23, 0, 0),
                LocalDateTime.of(2026, 6, 24, 0, 0)))
                .thenReturn(List.of(
                        shift("driver-1", LocalDateTime.of(2026, 6, 23, 10, 0)),
                        shift("driver-2", LocalDateTime.of(2026, 6, 23, 9, 0)),
                        shift("driver-1", LocalDateTime.of(2026, 6, 23, 8, 0)),
                        shift("driver-3", LocalDateTime.of(2026, 6, 23, 7, 0))));

        OperationalValidationSampleSelector.SelectedSample sample = selector.selectSample(properties);

        assertEquals(LocalDate.of(2026, 6, 23), sample.date());
        assertEquals(List.of("driver-1", "driver-2"), sample.driverIds());
        assertEquals(4, sample.scannedShiftCount());
    }

    @Test
    void selectorFallsBackToLatestManualShiftDate() {
        OperationalMonitoringRunnerProperties properties = new OperationalMonitoringRunnerProperties();
        when(repository.findLatestStartedAt()).thenReturn(LocalDateTime.of(2026, 6, 20, 14, 0));
        when(repository.findForSampleSelection(
                LocalDateTime.of(2026, 6, 20, 0, 0),
                LocalDateTime.of(2026, 6, 21, 0, 0)))
                .thenReturn(List.of(shift("driver-9", LocalDateTime.of(2026, 6, 20, 14, 0))));

        OperationalValidationSampleSelector.SelectedSample sample = selector.selectSample(properties);

        assertEquals(LocalDate.of(2026, 6, 20), sample.date());
        assertEquals(List.of("driver-9"), sample.driverIds());
    }

    private ShiftSession shift(String driverId, LocalDateTime startedAt) {
        return ShiftSession.builder()
                .id(UUID.randomUUID())
                .driverId(driverId)
                .startedAt(startedAt)
                .deleted(false)
                .build();
    }
}
