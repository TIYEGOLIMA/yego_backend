package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringProperties;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalManualComparisonResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationCoverageResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationMismatchResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationSummaryResponse;
import com.yego.backend.service.yego_pro_ops.OperationalShiftValidationService;
import com.yego.backend.service.yego_pro_ops.impl.OperationalDateRangeParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalShiftValidationControllerTest {

    @Mock
    private OperationalShiftValidationService validationService;

    private OperationalShiftValidationController controller;

    @BeforeEach
    void setUp() {
        controller = new OperationalShiftValidationController(
                validationService,
                new OperationalDateRangeParser(new OperationalMonitoringProperties("America/Lima", 8, 90, 90, "complete", true)));
    }

    @Test
    void forwardsManualComparisonLimitAndOffset() {
        when(validationService.getManualComparison(any(), any(), eq("driver-1"), eq("CAR-1"), eq(150), eq(20)))
                .thenReturn(List.<OperationalManualComparisonResponse>of());

        controller.getManualComparison("2026-06-25", "2026-06-26", "driver-1", "CAR-1", 150, 20);

        verify(validationService).getManualComparison(any(), any(), eq("driver-1"), eq("CAR-1"), eq(150), eq(20));
    }

    @Test
    void forwardsMismatchLimitAndOffset() {
        when(validationService.getMismatches(any(), any(), eq("driver-2"), eq("TIME_DELTA_HIGH"), eq(200), eq(0)))
                .thenReturn(List.<OperationalValidationMismatchResponse>of());

        controller.getMismatches("2026-06-25", "2026-06-26", "driver-2", "TIME_DELTA_HIGH", 200, 0);

        verify(validationService).getMismatches(any(), any(), eq("driver-2"), eq("TIME_DELTA_HIGH"), eq(200), eq(0));
    }

    @Test
    void requiresValidDateRange() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.getSummary("2026-06-27", "2026-06-25", null, null));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void exposesCoverageAndSummaryAsReadOnlyGets() {
        when(validationService.getSummary(any(), any(), any(), any()))
                .thenReturn(OperationalValidationSummaryResponse.builder().manualReplacementReadiness("WATCH").build());
        when(validationService.getCoverage(any(), any(), any(), any()))
                .thenReturn(OperationalValidationCoverageResponse.builder().vehicleKeyCoveragePct(80.0d).build());

        controller.getSummary("2026-06-25", "2026-06-25", null, null);
        controller.getCoverage("2026-06-25", "2026-06-25", null, null);

        verify(validationService).getSummary(any(), any(), any(), any());
        verify(validationService).getCoverage(any(), any(), any(), any());
    }
}
