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
public class DriverTripsMonthResponse {

    private String driverId;
    private Integer month;
    private Integer year;
    /** Viajes con condición Completado / Выполнен en el rango del mes (fecha_inicio_viaje). */
    private Integer completedTripsCount;
    /** Suma mensual de precio_yango_pro (soles). */
    private BigDecimal totalPrecioYangoProSoles;
    /** Un punto por día del mes (días sin viajes en 0). */
    private List<DailyTripsPointResponse> dailySeries;
    private List<TripCompletedItemResponse> trips;
}
