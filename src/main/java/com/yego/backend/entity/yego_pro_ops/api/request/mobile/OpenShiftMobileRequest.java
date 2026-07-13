package com.yego.backend.entity.yego_pro_ops.api.request.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenShiftMobileRequest {


    @NotBlank(message = "vehicleId es requerido")
    private String vehicleId;

    @NotBlank(message = "placa es requerida")
    @Pattern(regexp = "^[A-Za-z0-9-]{3,10}$", message = "Formato de placa inválido")
    private String placa;

    @NotNull(message = "kmInicial es requerido")
    @Positive(message = "kmInicial debe ser mayor a 0")
    private Integer kmInicial;

    private String selfieUri;

    private List<String> carPhotos;

    private String observaciones;

    private BigDecimal saldoAnterior;

    private String saldoDescripcion;
}
