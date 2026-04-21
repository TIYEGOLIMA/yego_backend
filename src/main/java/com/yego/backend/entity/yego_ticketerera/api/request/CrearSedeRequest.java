package com.yego.backend.entity.yego_ticketerera.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CrearSedeRequest {

    @NotBlank(message = "El nombre es requerido")
    private String name;

    private String description;
}
