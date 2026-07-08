package com.yego.backend.entity.yego_pro_ops.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OpenShiftMobileRequest {
    @NotBlank
    private String driverId;
    @NotBlank
    private String vehicleId;
    @NotBlank
    private String placa;
    private String modelo;
    @NotNull
    private Integer kmInicial;
    private String selfieUri;
    private List<String> carPhotos;
    private String observaciones;
    private Double saldoAnterior;
    private String saldoDescripcion;
}
