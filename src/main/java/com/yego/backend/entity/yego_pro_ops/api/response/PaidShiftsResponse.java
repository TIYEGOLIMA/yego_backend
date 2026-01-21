package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para listar los turnos que ya están pagados (pagado = true)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaidShiftsResponse {
    
    @JsonProperty("total_conductores")
    private Integer totalConductores; // Total de conductores con turnos pagados
    
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
        private String nombre; // Nombre completo del conductor
        
        @JsonProperty("telefono")
        private String telefono; // Teléfono del conductor
        
        @JsonProperty("cantidad_turnos")
        private Integer cantidadTurnos; // Cantidad de turnos pagados que tiene el conductor
        
        @JsonProperty("cantidad_viajes")
        private Integer cantidadViajes; // Total de viajes que tuvo el conductor en todos sus turnos pagados
        
        @JsonProperty("viajes_por_hora")
        private Double viajesPorHora; // KPI: cantidad de viajes por hora (cantidad_viajes / (duracion_total_minutos / 60))
        
        @JsonProperty("monto_total_pagado")
        private Double montoTotalPagado; // Suma de monto_total de los turnos pagados
        
        @JsonProperty("turnos")
        private List<TurnoPagadoInfo> turnos; // Lista de turnos pagados del conductor
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

