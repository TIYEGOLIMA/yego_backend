package com.yego.backend.entity.yego_premiun.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverMonthlyStatsProcessRequest {

    @NotNull(message = "month es obligatorio")
    @Min(value = 1, message = "month debe estar entre 1 y 12")
    @Max(value = 12, message = "month debe estar entre 1 y 12")
    private Integer month;

    @NotNull(message = "year es obligatorio")
    @Min(value = 2000, message = "year debe ser mayor o igual a 2000")
    private Integer year;
}

