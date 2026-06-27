package com.yego.backend.service.yego_pro_ops;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OperationalManualShiftReadAdapter {

    List<ManualShiftSnapshot> findManualShifts(LocalDateTime from, LocalDateTime to, String driverId, String vehicleKey);

    record ManualShiftSnapshot(
            UUID shiftSessionId,
            String driverId,
            LocalDateTime startedAt,
            LocalDateTime closedAt,
            String status,
            String plateNormalized) {
    }
}
