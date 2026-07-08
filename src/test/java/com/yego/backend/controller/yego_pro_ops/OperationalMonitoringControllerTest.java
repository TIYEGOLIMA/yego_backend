package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.OperationalShiftEventResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalShiftSessionResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalTripFactResponse;
import com.yego.backend.service.yego_pro_ops.OperationalShiftInferenceService;
import com.yego.backend.service.yego_pro_ops.OperationalTripFactService;
import com.yego.backend.service.yego_pro_ops.impl.OperationalDateRangeParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalMonitoringControllerTest {

    @Mock
    private OperationalTripFactService operationalTripFactService;
    @Mock
    private OperationalShiftInferenceService operationalShiftInferenceService;

    private OperationalMonitoringController controller;

    @BeforeEach
    void setUp() {
        OperationalDateRangeParser parser = new OperationalDateRangeParser(
                new com.yego.backend.config.yego_pro_ops.OperationalMonitoringProperties("America/Lima", 8, 90, 90, "complete", true));
        controller = new OperationalMonitoringController(operationalTripFactService, operationalShiftInferenceService, parser);
    }

    @Test
    void getShiftsForwardsLimitAndOffset() {
        when(operationalShiftInferenceService.searchShifts(any(), any(), eq("driver-1"), eq("CAR-1"), eq("OPEN_INFERRED"), eq(500), eq(25)))
                .thenReturn(List.<OperationalShiftSessionResponse>of());

        controller.getShifts("2026-06-25", "2026-06-26", "driver-1", "CAR-1", "OPEN_INFERRED", 500, 25);

        verify(operationalShiftInferenceService).searchShifts(any(), any(), eq("driver-1"), eq("CAR-1"), eq("OPEN_INFERRED"), eq(500), eq(25));
    }

    @Test
    void getEventsForwardsLimitAndOffset() {
        UUID shiftId = UUID.randomUUID();
        when(operationalShiftInferenceService.searchEvents(any(), any(), eq(shiftId), eq("driver-2"), eq("CAR-2"), eq(200), eq(0)))
                .thenReturn(List.<OperationalShiftEventResponse>of());

        controller.getEvents("2026-06-25", "2026-06-26", shiftId, "driver-2", "CAR-2", 200, 0);

        verify(operationalShiftInferenceService).searchEvents(any(), any(), eq(shiftId), eq("driver-2"), eq("CAR-2"), eq(200), eq(0));
    }

    @Test
    void tripFactsEndpointRemainsReadOnly() {
        when(operationalTripFactService.searchTripFacts(any(), any(), eq("driver-3"), eq("CAR-3"), eq("complete"), eq(50)))
                .thenReturn(List.<OperationalTripFactResponse>of());

        controller.getTripFacts("2026-06-25", "2026-06-26", "driver-3", "CAR-3", "complete", 50);

        verify(operationalTripFactService).searchTripFacts(any(), any(), eq("driver-3"), eq("CAR-3"), eq("complete"), eq(50));
    }

    @Test
    void controllerDoesNotDeclareCrossOriginWildcardOrPostReprocess() {
        assertNull(OperationalMonitoringController.class.getAnnotation(CrossOrigin.class));
        for (Method method : OperationalMonitoringController.class.getDeclaredMethods()) {
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping == null) {
                continue;
            }
            for (String path : postMapping.value()) {
                org.junit.jupiter.api.Assertions.assertNotEquals("/reprocess", path);
            }
        }
    }
}
