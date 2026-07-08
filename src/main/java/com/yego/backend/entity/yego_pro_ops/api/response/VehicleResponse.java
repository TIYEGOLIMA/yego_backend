package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    @JsonProperty("id")
    private String id;
    @JsonProperty("parkId")
    private String parkId;
    @JsonProperty("segmentId")
    private UUID segmentId;
    @JsonProperty("brand")
    private String brand;
    @JsonProperty("model")
    private String model;
    @JsonProperty("year")
    private Integer year;
    @JsonProperty("color")
    private String color;
    @JsonProperty("colorName")
    private String colorName;
    @JsonProperty("number")
    private String number;
    @JsonProperty("callsign")
    private String callsign;
    @JsonProperty("vin")
    private String vin;
    @JsonProperty("status")
    private YangoStatus status;
    @JsonProperty("categories")
    private List<String> categories;
    @JsonProperty("amenities")
    private List<String> amenities;
    @JsonProperty("mileage")
    private Integer mileage;
    @JsonProperty("rental")
    private Boolean rental;
    @JsonProperty("createdDate")
    private String createdDate;
    @JsonProperty("modifiedDate")
    private String modifiedDate;
    @JsonProperty("fotoUrl")
    private String fotoUrl;

    @JsonProperty("documents")
    private List<DocumentInfo> documents;
    @JsonProperty("maintenance")
    private List<MaintenanceInfo> maintenance;
    @JsonProperty("mileageHistory")
    private List<MileageInfo> mileageHistory;
    @JsonProperty("incidents")
    private List<IncidentInfo> incidents;
    @JsonProperty("qcHistory")
    private List<QcHistoryItem> qcHistory;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class YangoStatus {
        private String id;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocumentInfo {
        private Long id;
        private String tipo;
        private String nombre;
        private LocalDate fechaVigente;
        private String archivoUrl;
        private String estado;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MaintenanceInfo {
        private Long id;
        private String tipo;
        private String categoria;
        private LocalDate fecha;
        private BigDecimal kilometraje;
        private String descripcion;
        private String problema;
        private String diagnostico;
        private String solucion;
        private String taller;
        private String responsable;
        private BigDecimal costo;
        private String archivoUrl;
        private String estado;
        private LocalDate proximaFecha;
        private BigDecimal proximoKm;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MileageInfo {
        private Long id;
        private LocalDate fecha;
        private BigDecimal kilometraje;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IncidentInfo {
        private Long id;
        private LocalDate fecha;
        private String tipo;
        private String descripcion;
        private String conductor;
        private BigDecimal montoDano;
        private String estado;
        private String evidencias;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QcHistoryItem {
        private String exam;
        private String status;
        private String modified;
        private List<QcMedia> media;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QcMedia {
        private String code;
        private String name;
        private String url;
        private String status;
    }
}
