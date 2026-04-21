package com.yego.backend.entity.yego_ticketerera.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DispositivoAuthRequest {

    @NotBlank(message = "El accessToken es requerido")
    private String accessToken;
}
