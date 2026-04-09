package com.yego.backend.entity.yego_premiun.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCompletedItemResponse {

    private LocalDateTime fechaInicioViaje;
    private LocalDateTime fechaFinalizacion;
    private String condicion;
    private String parkId;
}
