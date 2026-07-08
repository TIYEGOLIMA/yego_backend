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
public class OperationalShiftEventResponse {
    private UUID id;
    private UUID operationalShiftSessionId;
    private String eventType;
    private LocalDateTime eventTime;
    private String driverId;
    private String vehicleKey;
    private String externalTripId;
    private String reason;
    private String details;
}
