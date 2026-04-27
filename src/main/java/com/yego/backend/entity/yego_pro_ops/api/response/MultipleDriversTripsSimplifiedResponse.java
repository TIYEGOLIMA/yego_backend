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
public class MultipleDriversTripsSimplifiedResponse {

    @JsonProperty("date_from")
    private String dateFrom;

    @JsonProperty("date_to")
    private String dateTo;

    @JsonProperty("drivers")
    private List<DriverTrips> drivers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverTrips {
        @JsonProperty("driver_id")
        private String driverId;

        @JsonProperty("trips")
        private List<TripSimplified> trips;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripSimplified {
        @JsonProperty("status")
        private String status;

        @JsonProperty("id")
        private String id;

        @JsonProperty("ended_at")
        private String endedAt;

        @JsonProperty("booked_at")
        private String bookedAt;
    }
}
