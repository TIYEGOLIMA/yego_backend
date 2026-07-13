package com.yego.backend.entity.yego_pro_ops.api.response.mobile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private long totalVehicles;
    private long activeVehicles;
    private long vehiclesInShift;
    private long vehiclesFree;
    private long vehiclesWithoutPhoto;
    private long vehiclesWithoutDocuments;
    private List<ActiveShiftItem> activeShifts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveShiftItem {
        private String sessionId;
        private String driverId;
        private String driverName;
        private String driverPhone;
        private String driverLicense;
        private String vehicleId;
        private String placa;
        private String modelo;
        private String startedAt;
        private String duration;
        private Integer kmInicial;
        private String status;
    }
}
