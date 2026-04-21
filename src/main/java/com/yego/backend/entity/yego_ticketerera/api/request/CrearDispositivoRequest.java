package com.yego.backend.entity.yego_ticketerera.api.request;

import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo.TipoDispositivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearDispositivoRequest {

    @NotBlank(message = "El nombre es requerido")
    private String name;

    @NotNull(message = "El tipo es requerido")
    private TipoDispositivo type;

    @NotNull(message = "El sede_id es requerido")
    private Long sedeId;

    private Long moduleId;

    private String description;
}
