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
public class PaidShiftsResponse {

    @JsonProperty("total_conductores")
    private Integer totalConductores;

    @JsonProperty("conductores")
    private List<ConductorTurnosPagadosInfo> conductores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConductorTurnosPagadosInfo {
        @JsonProperty("driver_id")
        private String driverId;

        @JsonProperty("avatar_url")
        private String avatarUrl;

        @JsonProperty("nombre")
        private String nombre;

        @JsonProperty("telefono")
        private String telefono;

        @JsonProperty("cantidad_turnos")
        private Integer cantidadTurnos;

        @JsonProperty("cantidad_viajes")
        private Integer cantidadViajes;

        @JsonProperty("viajes_por_hora")
        private Double viajesPorHora;

        @JsonProperty("monto_total_pagado")
        private Double montoTotalPagado;

        @JsonProperty("produccion_total")
        private Double produccionTotal;

        @JsonProperty("comisiones_servicio")
        private Double comisionesServicio;

        @JsonProperty("turnos")
        private List<TurnoPagadoInfo> turnos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TurnoPagadoInfo {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("fecha")
        private String fecha;

        @JsonProperty("hora_inicio")
        private String horaInicio;

        @JsonProperty("hora_fin")
        private String horaFin;

        @JsonProperty("tipo_turno")
        private String tipoTurno;

        @JsonProperty("duracion_minutos")
        private Integer duracionMinutos;

        @JsonProperty("monto_total")
        private Double montoTotal;

        @JsonProperty("pagado")
        private Boolean pagado;
    }
}
