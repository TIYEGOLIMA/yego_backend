package com.yego.backend.entity.yego_pro_ops.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoRequest {

    @NotNull(message = "hora_inicio es obligatorio")
    @JsonProperty("hora_inicio")
    private String horaInicio;

    @JsonProperty("hora_fin")
    private String horaFin;

    @NotNull(message = "tipo_turno es obligatorio")
    @JsonProperty("tipo_turno")
    private String tipoTurno;

    @JsonProperty("estado")
    private String estado;

    @JsonProperty("duracion_minutos")
    private Integer duracionMinutos;
}







