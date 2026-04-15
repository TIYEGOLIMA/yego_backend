package com.yego.backend.entity.yego_marketing_mensajes.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingMensajeRequest {

    @NotBlank(message = "El título es requerido")
    private String titulo;

    @NotBlank(message = "El mensaje es requerido")
    private String mensaje;

    private String modo;

    private String tipo;

    private String archivo;

    private Boolean whatsapp;

    private Boolean yandex;

    private List<String> diasActivos;

    private List<String> grupos;

    private List<String> flotas;

    private String horasEspecificas; // JSON string: {"18:00":["Jue"],"17:00":["Vie"]}

    private Boolean activo;
}

