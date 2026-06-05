package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionSemanalResponse {

    @JsonProperty("driverId")
    private String driverId;

    @JsonProperty("semanaInicio")
    private LocalDate semanaInicio;

    @JsonProperty("semanaFin")
    private LocalDate semanaFin;

    @JsonProperty("totalSesiones")
    private int totalSesiones;

    @JsonProperty("totalViajes")
    private int totalViajes;

    @JsonProperty("totalIngresos")
    private BigDecimal totalIngresos;

    @JsonProperty("totalKm")
    private BigDecimal totalKm;

    @JsonProperty("primerViaje")
    private String primerViaje;

    @JsonProperty("ultimoViaje")
    private String ultimoViaje;

    @JsonProperty("dias")
    private List<DiaLiquidacionInfo> dias;

    @JsonProperty("sesionesPendientes")
    private List<UUID> sesionesPendientes;

    @JsonProperty("tieneSesionesCerradas")
    private boolean tieneSesionesCerradas;

    @JsonProperty("tieneSesionActiva")
    private boolean tieneSesionActiva;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaLiquidacionInfo {

        @JsonProperty("fecha")
        private LocalDate fecha;

        @JsonProperty("diaSemana")
        private String diaSemana;

        @JsonProperty("viajes")
        private int viajes;

        @JsonProperty("ingresos")
        private BigDecimal ingresos;

        @JsonProperty("ingresosPendientes")
        private BigDecimal ingresosPendientes;

        @JsonProperty("ingresosLiquidados")
        private BigDecimal ingresosLiquidados;

        @JsonProperty("km")
        private BigDecimal km;

        @JsonProperty("sesiones")
        private int sesiones;

        @JsonProperty("estado")
        private String estado;

        @JsonProperty("sesionesDetalle")
        private List<SesionDiaInfo> sesionesDetalle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SesionDiaInfo {

        @JsonProperty("sessionId")
        private UUID sessionId;

        @JsonProperty("inicio")
        private String inicio;

        @JsonProperty("fin")
        private String fin;

        @JsonProperty("viajes")
        private int viajes;

        @JsonProperty("ingresos")
        private BigDecimal ingresos;

        @JsonProperty("km")
        private BigDecimal km;

        @JsonProperty("status")
        private String status;
    }
}
