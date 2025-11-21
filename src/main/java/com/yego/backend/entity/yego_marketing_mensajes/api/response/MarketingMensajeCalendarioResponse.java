package com.yego.backend.entity.yego_marketing_mensajes.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta de mensaje de marketing para el calendario
 * Solo incluye los campos necesarios para mostrar en el calendario semanal
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingMensajeCalendarioResponse {

    private Long id;
    private String titulo;
    private String modo;
    private List<String> diasActivos;
    private String horasEspecificas; // JSON string: {"18:00":["Jue"],"17:00":["Vie"]}
}



