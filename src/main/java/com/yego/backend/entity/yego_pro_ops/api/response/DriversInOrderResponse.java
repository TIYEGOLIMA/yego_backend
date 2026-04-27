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

        @JsonProperty("vehicle_number")
        private String vehicleNumber;

        @JsonProperty("viajes")
        private List<TripSimplified> viajes;

        @JsonProperty("summary_distance")
        private SummaryDistance summaryDistance;

        @JsonProperty("total_activity_time")
        private Long totalActivityTime;

        @JsonProperty("completed_trips_count")
        private Integer completedTripsCount;

        @JsonProperty("completed_trips_total_price")
        private Double completedTripsTotalPrice;
    }

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDistance {
        @JsonProperty("free")
        private Double free;

        @JsonProperty("not_active")
        private Double notActive;

        @JsonProperty("active")
        private Double active;
    }
}
