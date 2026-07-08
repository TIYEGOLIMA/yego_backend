package com.yego.backend.entity.yego_pro_ops.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CloseShiftMobileRequest {
    private Long userId;
    @NotNull
    private Integer kmFinal;
    private Double efectivo;
    private Double yape;
    private String numeroOperacion;
    private Double gasolinaMonto;
    private Double gasolinaGalones;
    private Double gnvMonto;
    private Double gnvM3;
    private Double otrosGastos;
    private String otrosGastosDescripcion;
    private List<String> carPhotosCierre;
    private String observaciones;
    private Boolean mantenimientoRequerido;
    private String mantenimientoDescripcion;
}
