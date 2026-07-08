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
public class OperationalTripFactResponse {
    private UUID id;
    private String externalTripId;
    private String driverId;
    private String vehicleKey;
    private String vehicleKeySource;
    private String vehicleId;
    private String vehiclePlate;
    private String vehiclePlateNormalized;
    private String tripStatus;
    private LocalDateTime bookedAt;
    private LocalDateTime endedAt;
    private LocalDateTime observedAt;
    private String source;
}
