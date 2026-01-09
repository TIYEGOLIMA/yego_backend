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
public class DriverTripsSimplifiedResponse {
    
    @JsonProperty("date_from")
    private String dateFrom;
    
    @JsonProperty("date_to")
    private String dateTo;
    
    @JsonProperty("trips")
    private List<TripSimplified> trips;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripSimplified {
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("short_id")
        private Long shortId;
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("driver_id")
        private String driverId;
        
        @JsonProperty("driver_full_name")
        private String driverFullName;
        
        @JsonProperty("ended_at")
        private String endedAt;
        
        @JsonProperty("booked_at")
        private String bookedAt;
        
        @JsonProperty("car_brand_model")
        private String carBrandModel;
    }
}

