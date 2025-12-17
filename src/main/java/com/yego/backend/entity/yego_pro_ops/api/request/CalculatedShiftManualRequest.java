package com.yego.backend.entity.yego_pro_ops.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculatedShiftManualRequest {

    @NotNull(message = "driverId es requerido")
    @NotBlank(message = "driverId no puede estar vacío")
    @JsonProperty("driverId")
    private String driverId;

    @NotNull(message = "fecha es requerida")
    @NotBlank(message = "fecha no puede estar vacía")
    @JsonProperty("fecha")
    private String fecha;

    @NotNull(message = "horaInicio es requerida")
    @NotBlank(message = "horaInicio no puede estar vacía")
    @JsonProperty("horaInicio")
    private String horaInicio;

    @JsonProperty("horaFin")
    private String horaFin;

    @NotNull(message = "tipoTurno es requerido")
    @NotBlank(message = "tipoTurno no puede estar vacío")
    @JsonProperty("tipoTurno")
    private String tipoTurno;
}

