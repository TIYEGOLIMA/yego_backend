package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Vehículo cacheado devuelto en el listado de flota.
 * Nombres en snake_case para mantener compatibilidad con el tipo YangoVehicle del frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetVehicleResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("segment_id")
    private UUID segmentId;

    @JsonProperty("park_id")
    private String parkId;

    @JsonProperty("park_nombre")
    private String parkNombre;

    @JsonProperty("number")
    private String number;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("model")
    private String model;

    @JsonProperty("year")
    private Integer year;

    @JsonProperty("color")
    private String color;

    @JsonProperty("color_name")
    private String colorName;

    @JsonProperty("vin")
    private String vin;

    @JsonProperty("callsign")
    private String callsign;

    @JsonProperty("status")
    private VehicleResponse.YangoStatus status;

    @JsonProperty("categories")
    private List<String> categories;

    @JsonProperty("amenities")
    private List<String> amenities;

    @JsonProperty("mileage")
    private Integer mileage;

    @JsonProperty("rental")
    private Boolean rental;

    @JsonProperty("foto_url")
    private String fotoUrl;
}
