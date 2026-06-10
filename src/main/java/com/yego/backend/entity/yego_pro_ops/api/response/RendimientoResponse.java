package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RendimientoResponse {

    @JsonProperty("periodo")
    private String periodo;

    @JsonProperty("desde")
    private LocalDate desde;

    @JsonProperty("hasta")
    private LocalDate hasta;

    @JsonProperty("totales")
    private TotalesRendimiento totales;

    @JsonProperty("tendenciaDiaria")
    private List<TendenciaDiaria> tendenciaDiaria;

    @JsonProperty("conductores")
    private List<ConductorRendimiento> conductores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalesRendimiento {
        @JsonProperty("conductores")
        private int conductores;

        @JsonProperty("viajes")
        private int viajes;

        @JsonProperty("efectivo")
        private BigDecimal efectivo;

        @JsonProperty("yape")
        private BigDecimal yape;

        @JsonProperty("montoTotalProducido")
        private BigDecimal montoTotalProducido;

        @JsonProperty("km")
        private BigDecimal km;

        @JsonProperty("gnvSoles")
        private BigDecimal gnvSoles;

        @JsonProperty("gasolinaSoles")
        private BigDecimal gasolinaSoles;

        @JsonProperty("horas")
        private BigDecimal horas;

        @JsonProperty("viajesPorHora")
        private BigDecimal viajesPorHora;

        @JsonProperty("minimoViajes")
        private int minimoViajes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TendenciaDiaria {
        @JsonProperty("fecha")
        private String fecha;

        @JsonProperty("viajes")
        private int viajes;

        @JsonProperty("efectivo")
        private BigDecimal efectivo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConductorRendimiento {
        @JsonProperty("driverId")
        private String driverId;

        @JsonProperty("nombre")
        private String nombre;

        @JsonProperty("totalViajes")
        private int totalViajes;

        @JsonProperty("totalEfectivo")
        private BigDecimal totalEfectivo;

        @JsonProperty("totalYape")
        private BigDecimal totalYape;

        @JsonProperty("totalProducido")
        private BigDecimal totalProducido;

        @JsonProperty("totalKm")
        private BigDecimal totalKm;

        @JsonProperty("totalGnvSoles")
        private BigDecimal totalGnvSoles;

        @JsonProperty("totalGasolinaSoles")
        private BigDecimal totalGasolinaSoles;

        @JsonProperty("totalOtrosGastos")
        private BigDecimal totalOtrosGastos;

        @JsonProperty("totalHoras")
        private BigDecimal totalHoras;

        @JsonProperty("viajesPorHora")
        private BigDecimal viajesPorHora;

        @JsonProperty("rentabilidad")
        private BigDecimal rentabilidad;
    }
}
