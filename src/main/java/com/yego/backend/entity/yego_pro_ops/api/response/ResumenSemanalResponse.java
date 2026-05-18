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
public class ResumenSemanalResponse {

    @JsonProperty("fecha_inicio")
    private String fechaInicio;

    @JsonProperty("fecha_fin")
    private String fechaFin;

    @JsonProperty("total_conductores")
    private int totalConductores;

    @JsonProperty("total_viajes")
    private int totalViajes;

    @JsonProperty("total_produccion")
    private double totalProduccion;

    @JsonProperty("total_comision")
    private double totalComision;

    @JsonProperty("total_combustible")
    private double totalCombustible;

    @JsonProperty("total_pagar")
    private double totalPagar;

    @JsonProperty("total_pagado")
    private double totalPagado;

    @JsonProperty("total_pendiente")
    private double totalPendiente;

    @JsonProperty("total_bonos")
    private double totalBonos;

    @JsonProperty("total_utilidad")
    private double totalUtilidad;

    @JsonProperty("total_turnos")
    private int totalTurnos;

    @JsonProperty("conductores")
    private List<ConductorSemanalInfo> conductores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConductorSemanalInfo {

        @JsonProperty("driver_id")
        private String driverId;

        @JsonProperty("avatar_url")
        private String avatarUrl;

        @JsonProperty("nombre")
        private String nombre;

        @JsonProperty("telefono")
        private String telefono;

        @JsonProperty("placa")
        private String placa;

        @JsonProperty("turno")
        private String turno;

        @JsonProperty("dias_trabajados")
        private int diasTrabajados;

        @JsonProperty("dias_liquidados")
        private int diasLiquidados;

        @JsonProperty("total_viajes")
        private int totalViajes;

        @JsonProperty("viajes_validos")
        private int viajesValidos;

        @JsonProperty("horas_trabajo")
        private double horasTrabajo;

        @JsonProperty("tph")
        private double tph;

        @JsonProperty("monto_total_producido")
        private double montoTotalProducido;

        @JsonProperty("comision_app")
        private double comisionApp;

        @JsonProperty("monto_neto")
        private double montoNeto;

        @JsonProperty("km_recorrido")
        private double kmRecorrido;

        @JsonProperty("gasto_combustible")
        private double gastoCombustible;

        @JsonProperty("bono_yango")
        private double bonoYango;

        @JsonProperty("gasto_mantenimiento")
        private double gastoMantenimiento;

        @JsonProperty("produccion_bonificable")
        private double produccionBonificable;

        @JsonProperty("bono_adic_viajes")
        private double bonoAdicViajes;

        @JsonProperty("bono")
        private double bono;

        @JsonProperty("porcentaje_pago")
        private double porcentajePago;

        @JsonProperty("pago")
        private double pago;

        @JsonProperty("pago_total")
        private double pagoTotal;

        @JsonProperty("total_pagado")
        private double totalPagado;

        @JsonProperty("utilidad")
        private double utilidad;

        @JsonProperty("utilidad_por_viaje")
        private double utilidadPorViaje;

        @JsonProperty("pago_por_viaje")
        private double pagoPorViaje;

        @JsonProperty("completamente_liquidado")
        private boolean completamenteLiquidado;

        @JsonProperty("datos_por_dia")
        private List<DiaSemanalInfo> datosPorDia;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaSemanalInfo {

        @JsonProperty("fecha")
        private String fecha;

        @JsonProperty("dia_semana")
        private String diaSemana;

        @JsonProperty("cantidad_viajes")
        private int cantidadViajes;

        @JsonProperty("cantidad_turnos")
        private int cantidadTurnos;

        @JsonProperty("turnos_tipo")
        private String turnosTipo;

        @JsonProperty("produccion_total")
        private double produccionTotal;

        @JsonProperty("comisiones_servicio")
        private double comisionesServicio;

        @JsonProperty("monto_total_pagar")
        private double montoTotalPagar;

        @JsonProperty("monto_total_pagado")
        private double montoTotalPagado;

        @JsonProperty("gasto_combustible")
        private double gastoCombustible;

        @JsonProperty("liquida_efectivo")
        private double liquidaEfectivo;

        @JsonProperty("liquida_yape")
        private double liquidaYape;

        @JsonProperty("otros_gastos")
        private double otrosGastos;

        @JsonProperty("odometro_inicial")
        private Integer odometroInicial;

        @JsonProperty("odometro_final")
        private Integer odometroFinal;

        @JsonProperty("km_recorrido")
        private double kmRecorrido;

        @JsonProperty("liquidado")
        private boolean liquidado;
    }
}
