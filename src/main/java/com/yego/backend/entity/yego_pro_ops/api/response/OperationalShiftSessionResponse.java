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
public class OperationalShiftSessionResponse {
    private UUID id;
    private String driverId;
    private String vehicleKey;
    private String vehicleKeySource;
    private String vehicleId;
    private String vehiclePlateNormalized;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private String state;
    private String openReason;
    private String closeReason;
    private String firstTripExternalId;
    private String lastTripExternalId;
    private Integer tripCount;
    private LocalDateTime lastActivityAt;
    private String confidenceLevel;
    private Boolean needsReview;
    private String reviewReason;
}
