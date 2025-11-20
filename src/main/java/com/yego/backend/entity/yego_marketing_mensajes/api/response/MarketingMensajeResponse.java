package com.yego.backend.entity.yego_marketing_mensajes.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de mensaje de marketing
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingMensajeResponse {

    private Long id;
    private Long userId;
    private String titulo;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String mensajeOperacion;
}

