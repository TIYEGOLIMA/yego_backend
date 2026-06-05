package com.yego.backend.entity.yego_pro_ops.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTripRequest {

    @NotBlank(message = "driverId es requerido")
    @JsonProperty("driverId")
    private String driverId;

    @JsonProperty("externalTripId")
    private String externalTripId;

    @NotBlank(message = "completedAt es requerido")
    @JsonProperty("completedAt")
    private String completedAt;

    @JsonProperty("amount")
    private BigDecimal amount;
}
