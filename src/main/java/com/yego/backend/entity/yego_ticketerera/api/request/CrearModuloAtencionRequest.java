package com.yego.backend.entity.yego_ticketerera.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearModuloAtencionRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    private String name;

    private String description;

    @NotNull(message = "El sede_id es requerido")
    private Long sedeId;
}
