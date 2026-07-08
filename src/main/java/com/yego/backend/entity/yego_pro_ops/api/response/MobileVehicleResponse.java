package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta agregada para la app móvil: todo del auto por placa en una sola llamada.
 * Se construye desde BD local (rápida, sin Yango ni API externa).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileVehicleResponse {

    private General general;
    private String imagen;
    private List<VehicleResponse.DocumentInfo> documentos;
    private List<VehicleResponse.MaintenanceInfo> mantenimiento;
    private List<VehicleResponse.MileageInfo> kilometraje;
    private List<VehicleResponse.IncidentInfo> siniestros;
    private Gasto gastos;
    private List<VehicleTraceEvent> trazabilidad;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class General {
        private String yangoCarId;
        private String placa;
        private String marca;
        private String modelo;
        private Integer anio;
        private String color;
        private String vin;
        private String estado;
        private String flota;
        private List<String> categorias;
        private List<String> amenities;
        private Boolean rental;
        private Integer kilometraje;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Gasto {
        private BigDecimal total;
        private BigDecimal preventivo;
        private BigDecimal correctivo;
        private BigDecimal siniestros;
        private double pctPreventivo;
        private double pctCorrectivo;
        private double pctSiniestros;
        private List<GastoMes> porMes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GastoMes {
        private String mes;
        private BigDecimal monto;
    }
}
