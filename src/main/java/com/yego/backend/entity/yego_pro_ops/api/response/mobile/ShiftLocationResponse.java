package com.yego.backend.entity.yego_pro_ops.api.response.mobile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftLocationResponse {
    private String sessionId;
    private String driverId;
    private String driverName;
    private String vehicleId;
    private String placa;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Double speed;
    private Double heading;
    private Instant recordedAt;
    private String source;
}
