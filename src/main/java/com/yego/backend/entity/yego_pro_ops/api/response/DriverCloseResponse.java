package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverCloseResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("driverId")
    private String driverId;

    @JsonProperty("fecha")
    private LocalDate fecha;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("userIdModificado")
    private Long userIdModificado;

    @JsonProperty("userNameModificado")
    private String userNameModificado;

    @JsonProperty("gnvM3")
    private String gnvM3;

    @JsonProperty("gnvSoles")
    private BigDecimal gnvSoles;

    @JsonProperty("gasolinaGalones")
    private String gasolinaGalones;

    @JsonProperty("gasolinaSoles")
    private BigDecimal gasolinaSoles;

    @JsonProperty("liquidaEfectivo")
    private BigDecimal liquidaEfectivo;

    @JsonProperty("liquidaYape")
    private BigDecimal liquidaYape;

    @JsonProperty("operacionYape")
    private String operacionYape;

    @JsonProperty("otrosGastos")
    private BigDecimal otrosGastos;

    @JsonProperty("otrosGastosDescripcion")
    private String otrosGastosDescripcion;

    @JsonProperty("totalIngresos")
    private BigDecimal totalIngresos;

    @JsonProperty("totalGastos")
    private BigDecimal totalGastos;

    @JsonProperty("resta")
    private BigDecimal resta;

    @JsonProperty("placa")
    private String placa;

    @JsonProperty("odometroInicial")
    private Integer odometroInicial;

    @JsonProperty("odometroFinal")
    private Integer odometroFinal;

    @JsonProperty("diferenciaOdometro")
    private Integer diferenciaOdometro;

    @JsonProperty("shiftSessionId")
    private UUID shiftSessionId;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
