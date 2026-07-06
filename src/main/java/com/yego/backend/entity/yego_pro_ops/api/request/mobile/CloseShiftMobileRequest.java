package com.yego.backend.entity.yego_pro_ops.api.request.mobile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloseShiftMobileRequest {

    @NotNull(message = "kmFinal es requerido")
    @Positive(message = "kmFinal debe ser mayor a 0")
    private Integer kmFinal;

    @NotNull(message = "efectivo es requerido")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal efectivo;

    @NotNull(message = "yape es requerido")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal yape;

    private String numeroOperacion;

    private BigDecimal gasolinaMonto;

    private BigDecimal gasolinaGalones;

    private BigDecimal gnvMonto;

    private BigDecimal gnvM3;

    private BigDecimal otrosGastos;

    private String otrosGastosDescripcion;

    private List<String> carPhotosCierre;

    private String observaciones;

    private Boolean mantenimientoRequerido;

    private String mantenimientoDescripcion;
}
