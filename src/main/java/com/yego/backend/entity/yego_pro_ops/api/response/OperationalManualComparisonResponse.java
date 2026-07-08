package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalManualComparisonResponse {
    private UUID operationalShiftId;
    private UUID manualShiftId;
    private String driverId;
    private String comparisonStatus;
    private String mismatchReason;
    private LocalDateTime operationalOpenedAt;
    private LocalDateTime operationalClosedAt;
    private String operationalState;
    private String operationalVehicleKey;
    private String operationalPlateNormalized;
    private LocalDateTime manualStartedAt;
    private LocalDateTime manualClosedAt;
    private String manualStatus;
    private String manualPlateNormalized;
    private String vehicleComparison;
    private Long startDeltaMinutes;
    private Long endDeltaMinutes;
}
