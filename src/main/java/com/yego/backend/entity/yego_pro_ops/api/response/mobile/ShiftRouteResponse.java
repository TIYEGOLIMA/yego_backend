package com.yego.backend.entity.yego_pro_ops.api.response.mobile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftRouteResponse {
    private String sessionId;
    private String driverId;
    private String driverName;
    private String vehicleId;
    private String placa;
    private Instant startedAt;
    private Instant closedAt;
    private String status;
    private List<ShiftLocationResponse> points;
}
