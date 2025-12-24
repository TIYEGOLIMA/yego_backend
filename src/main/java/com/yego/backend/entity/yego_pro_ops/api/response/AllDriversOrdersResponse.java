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
public class AllDriversOrdersResponse {
    
    @JsonProperty("fecha")
    private String fecha;
    
    @JsonProperty("conductores")
    private List<DriverWithOrders> conductores;
    
    @JsonProperty("total_conductores")
    private Integer totalConductores;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverWithOrders {
        @JsonProperty("driver_id")
        private String driverId;
        
        @JsonProperty("full_name")
        private String fullName;
        
        @JsonProperty("phone")
        private String phone;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("viajes")
        private List<OrderInfoResponse> viajes;
        
        @JsonProperty("total_viajes")
        private Integer totalViajes;
    }
}







