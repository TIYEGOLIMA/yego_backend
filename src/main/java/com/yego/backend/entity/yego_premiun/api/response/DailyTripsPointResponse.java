package com.yego.backend.entity.yego_premiun.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTripsPointResponse {

    /** Día del mes (viajes completados ese día). */
    private LocalDate date;
    private Integer tripsCount;
    /** Suma de {@code precio_yango_pro} del día en soles (según {@code yego.premiun.yango-pro-divisor} respecto a la columna en BD). */
    private BigDecimal precioYangoProSoles;
}
