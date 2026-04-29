package com.yego.backend.entity.yego_gantt.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta de {@code GET /tasks/summary}: listado de tareas más agregados KPI en un solo objeto JSON.
 * Los KPI no se exponen como tipo de nivel superior: solo tienen sentido junto al listado filtrado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaTasksSummaryResponseDto {

    private List<AreaTaskResponseDto> tasks;

    /** Mismas claves que el tipo {@code Kpis} del frontend ({@code equipos}, {@code tareas}, …). */
    private SummaryKpis kpis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryKpis {
        private int equipos;
        private int tareas;
        private double progresoPromedioPct;
        private int completadas;
        private int bloqueadas;
    }
}
