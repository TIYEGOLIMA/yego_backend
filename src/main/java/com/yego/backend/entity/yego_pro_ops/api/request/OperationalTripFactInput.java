package com.yego.backend.entity.yego_pro_ops.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalTripFactInput {
    private String externalTripId;
    private String driverId;
    private String vehicleKey;
    private String vehicleKeySource;
    private String vehicleId;
    private String vehiclePlate;
    private String tripStatus;
    private LocalDateTime bookedAt;
    private LocalDateTime endedAt;
    private LocalDateTime observedAt;
    private String source;
    private String sourcePayloadHash;
}
