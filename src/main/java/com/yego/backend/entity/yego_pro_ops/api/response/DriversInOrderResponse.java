package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriversInOrderResponse {
    
    @JsonProperty("conductores")
    private List<DriverInOrderInfo> conductores;
    
    @JsonProperty("total")
    private Integer total;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInOrderInfo {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("avatar_url")
        private String avatarUrl;
        
        @JsonProperty("balance")
        private String balance;
        
        @JsonProperty("first_name")
        private String firstName;
        
        @JsonProperty("last_name")
        private String lastName;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("route")
        private List<RoutePoint> route;
        
        @JsonProperty("vehicle_number")
        private String vehicleNumber;
        
        @JsonProperty("summary_distance")
        private SummaryDistance summaryDistance;
        
        @JsonProperty("total_activity_time")
        private Long totalActivityTime; // Tiempo total de actividad en segundos (suma de status_time de todos los viajes)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutePoint {
        @JsonProperty("address")
        private String address;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDistance {
        @JsonProperty("common")
        private Double common;
        
        @JsonProperty("active")
        private Double active;
        
        @JsonProperty("not_active")
        private Double notActive;
        
        @JsonProperty("offline")
        private Double offline;
        
        @JsonProperty("busy")
        private Double busy;
        
        @JsonProperty("free")
        private Double free;
        
        @JsonProperty("in_order")
        private Double inOrder;
    }
}



