package com.yego.backend.entity.yego_pro_ops.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverCloseRequest {

    @JsonProperty("id")
    private Long id;

    @NotBlank(message = "driverId es requerido")
    @JsonProperty("driverId")
    private String driverId;

    @NotBlank(message = "fecha es requerida")
    @JsonProperty("fecha")
    private String fecha;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("gnvM3")
    private String gnvM3;

    @JsonProperty("gnvSoles")
    private Double gnvSoles;

    @JsonProperty("gasolinaGalones")
    private String gasolinaGalones;

    @JsonProperty("gasolinaSoles")
    private Double gasolinaSoles;

    @JsonProperty("liquidaEfectivo")
    private Double liquidaEfectivo;

    @JsonProperty("liquidaYape")
    private Double liquidaYape;

    @JsonProperty("otrosGastos")
    private Double otrosGastos;

    @JsonProperty("otrosGastosDescripcion")
    private String otrosGastosDescripcion;

    @JsonProperty("totalIngresos")
    private Double totalIngresos;

    @JsonProperty("totalGastos")
    private Double totalGastos;

    @JsonProperty("resta")
    private Double resta;

    @JsonProperty("placa")
    private String placa;

    @JsonProperty("odometroInicial")
    private Integer odometroInicial;

    @JsonProperty("odometroFinal")
    private Integer odometroFinal;

    @JsonProperty("diferenciaOdometro")
    private Integer diferenciaOdometro;

    @JsonProperty("turnoIds")
    private List<Long> turnoIds;
}
