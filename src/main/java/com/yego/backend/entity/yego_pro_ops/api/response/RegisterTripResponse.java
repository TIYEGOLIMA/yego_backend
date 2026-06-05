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
public class RegisterTripResponse {

    @JsonProperty("trip")
    private TripResponse trip;

    @JsonProperty("sessionOpened")
    private boolean sessionOpened;

    @JsonProperty("session")
    private ShiftSessionResponse session;
}
