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
        
        @JsonProperty("ended_at")
        private String endedAt;
        
        @JsonProperty("booked_at")
        private String bookedAt;
    }
}

