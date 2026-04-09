package com.yego.backend.entity.yego_premiun.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTripsAggregateResponse {

    private Integer month;
    private Integer tripsCount;
    private BigDecimal precioYangoProSoles;
}
