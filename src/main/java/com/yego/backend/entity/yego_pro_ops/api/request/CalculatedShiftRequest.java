package com.yego.backend.entity.yego_pro_ops.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculatedShiftRequest {

    @NotBlank(message = "driver_id es obligatorio")
    @JsonProperty("driver_id")
    private String driverId;

    @NotBlank(message = "fecha es obligatorio")
    @JsonProperty("fecha")
    private String fecha;

    @NotEmpty(message = "turnos no puede estar vacío")
    @JsonProperty("turnos")
    @Valid
    private List<TurnoRequest> turnos;
}

