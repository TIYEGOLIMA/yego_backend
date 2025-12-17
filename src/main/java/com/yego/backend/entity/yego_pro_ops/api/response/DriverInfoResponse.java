package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverInfoResponse {
    
    @JsonProperty("driver_id")
    private String driverId;
    
    @JsonProperty("full_name")
    private String fullName;
    
    @JsonProperty("car_number")
    private String carNumber;
    
    @JsonProperty("coordinates")
    private CoordinatesResponse coordinates;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("balance")
    private Double balance;
    
    @JsonProperty("status_duration")
    private Integer statusDuration;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoordinatesResponse {
        @JsonProperty("lon")
        private Double lon;
        
        @JsonProperty("lat")
        private Double lat;
    }
}

