package com.yego.backend.entity.yego_marketing_mensajes.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingMensajeRequest {

    @NotBlank(message = "El título es requerido")
    @Size(max = 255, message = "El título no puede superar 255 caracteres")
    private String titulo;

    @NotBlank(message = "El mensaje es requerido")
    private String mensaje;

    @Size(max = 50, message = "El modo no puede superar 50 caracteres")
    private String modo;

    @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
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
