package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculatedShiftResponse {

    @JsonProperty("driver_id")
    private String driverId;

    @JsonProperty("fecha")
    private String fecha;

    @JsonProperty("turnos")
    private List<TurnoResponse> turnos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TurnoResponse {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("hora_inicio")
        private String horaInicio;

        @JsonProperty("hora_fin")
        private String horaFin;

        @JsonProperty("tipo_turno")
        private String tipoTurno;

        @JsonProperty("estado")
        private String estado;

        @JsonProperty("duracion_minutos")
        private Integer duracionMinutos;
    }
}

