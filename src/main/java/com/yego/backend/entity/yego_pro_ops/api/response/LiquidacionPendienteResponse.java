package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquidacionPendienteResponse {

    @JsonProperty("driverId")
    private String driverId;

    @JsonProperty("periodoDesde")
    private LocalDateTime periodoDesde;

    @JsonProperty("periodoHasta")
    private LocalDateTime periodoHasta;

    @JsonProperty("esPrimeraLiquidacion")
    private boolean esPrimeraLiquidacion;

    @JsonProperty("totalSesiones")
    private int totalSesiones;

    @JsonProperty("totalViajes")
    private int totalViajes;

    @JsonProperty("viajesPorHora")
    private BigDecimal viajesPorHora;

    @JsonProperty("kmRecorrido")
    private BigDecimal kmRecorrido;

    @JsonProperty("montoTotalProducido")
    private BigDecimal montoTotalProducido;

    @JsonProperty("placa")
    private String placa;

    @JsonProperty("carBrandModel")
    private String carBrandModel;

    @JsonProperty("semanaCerrada")
    private boolean semanaCerrada;

    @JsonProperty("bonoYango")
    private BigDecimal bonoYango;

    @JsonProperty("comisionApp")
    private BigDecimal comisionApp;

    @JsonProperty("montoNeto")
    private BigDecimal montoNeto;

    @JsonProperty("produccionBonificable")
    private BigDecimal produccionBonificable;

    @JsonProperty("bono")
    private BigDecimal bono;

    @JsonProperty("porcentajePago")
    private Double porcentajePago;

    @JsonProperty("pago")
    private BigDecimal pago;

    @JsonProperty("pagoTotal")
    private BigDecimal pagoTotal;

    @JsonProperty("efectivo")
    private BigDecimal efectivo;

    @JsonProperty("utilidad")
    private BigDecimal utilidad;

    @JsonProperty("utilidadPorViaje")
    private BigDecimal utilidadPorViaje;

    @JsonProperty("pagoPorViaje")
    private BigDecimal pagoPorViaje;

    @JsonProperty("diasTrabajados")
    private int diasTrabajados;

    @JsonProperty("sesionesPendientes")
    private List<UUID> sesionesPendientes;

    @JsonProperty("dias")
    private List<DiaPendienteInfo> dias;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaPendienteInfo {

        @JsonProperty("fecha")
        private String fecha;

        @JsonProperty("diaSemana")
        private String diaSemana;

        @JsonProperty("sesiones")
        private int sesiones;

        @JsonProperty("viajes")
        private int viajes;

        @JsonProperty("ingresos")
        private BigDecimal ingresos;

        @JsonProperty("km")
        private BigDecimal km;

        @JsonProperty("estado")
        private String estado;

        @JsonProperty("sesionesDetalle")
        private List<SesionDiaInfo> sesionesDetalle;
    }
}
