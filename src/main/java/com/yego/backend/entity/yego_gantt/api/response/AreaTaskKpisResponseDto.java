package com.yego.backend.entity.yego_gantt.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaTaskKpisResponseDto {
    private int equipos;
    private int tareas;
    private double progresoPromedioPct;
    private int completadas;
    private int enRiesgo;
    private int bloqueadas;
}
