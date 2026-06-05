package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftSessionResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("driverId")
    private String driverId;

    @JsonProperty("startedAt")
    private LocalDateTime startedAt;

    @JsonProperty("closedAt")
    private LocalDateTime closedAt;

    @JsonProperty("settledAt")
    private LocalDateTime settledAt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("totalTrips")
    private Integer totalTrips;

    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
