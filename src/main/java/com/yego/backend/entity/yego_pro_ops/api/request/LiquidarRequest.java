package com.yego.backend.entity.yego_pro_ops.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquidarRequest {

    @JsonProperty("driverId")
    private String driverId;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("desde")
    private String desde;

    @JsonProperty("hasta")
    private String hasta;

    @JsonProperty("placa")
    private String placa;

    @JsonProperty("odometroInicial")
    private Integer odometroInicial;

    @JsonProperty("odometroFinal")
    private Integer odometroFinal;

    @JsonProperty("diferenciaOdometro")
    private Integer diferenciaOdometro;

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

    @JsonProperty("operacionYape")
    private String operacionYape;

    @JsonProperty("adelanto")
    private Double adelanto;

    @JsonProperty("montoTotalProducido")
    private Double montoTotalProducido;
}
