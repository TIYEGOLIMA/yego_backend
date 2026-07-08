package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.OperationalShiftEventResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalShiftSessionResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OperationalShiftInferenceService {

    ReprocessResult reprocessRange(LocalDateTime from, LocalDateTime to, String driverId, String vehicleKey);

    List<OperationalShiftSessionResponse> searchShifts(
            LocalDateTime from,
            LocalDateTime to,
            String driverId,
            String vehicleKey,
            String state,
            Integer limit,
            Integer offset);

    List<OperationalShiftEventResponse> searchEvents(
            LocalDateTime from,
            LocalDateTime to,
            UUID shiftId,
            String driverId,
            String vehicleKey,
            Integer limit,
            Integer offset);

    record ReprocessResult(int tripFactsConsidered, int sessionsCreated, int eventsCreated) {
    }
}
