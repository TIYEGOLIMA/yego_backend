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
public class TripResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("driverId")
    private String driverId;

    @JsonProperty("shiftSessionId")
    private UUID shiftSessionId;

    @JsonProperty("externalTripId")
    private String externalTripId;

    @JsonProperty("completedAt")
    private LocalDateTime completedAt;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
