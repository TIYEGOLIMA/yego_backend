package com.yego.backend.entity.yego_premiun.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverTripsYearResponse {

    private String driverId;
    private Integer year;
    private Integer totalCompletedTrips;
    private BigDecimal totalPrecioYangoProSoles;
    /** Enero (=1) a diciembre (=12); meses sin datos en 0. */
    private List<MonthlyTripsAggregateResponse> monthlySeries;
}
