package com.yego.backend.entity.yego_pro_ops.api.request.mobile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileShiftLocationRequest {


    @NotNull(message = "latitude es requerida")
    @DecimalMin(value = "-90.0", inclusive = true)
    @DecimalMax(value = "90.0", inclusive = true)
    private Double latitude;

    @NotNull(message = "longitude es requerida")
    @DecimalMin(value = "-180.0", inclusive = true)
    @DecimalMax(value = "180.0", inclusive = true)
    private Double longitude;

    private Double accuracy;
    private Double speed;
    private Double heading;
    private Instant recordedAt;
    private String source;
}
