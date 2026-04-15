package com.yego.backend.entity.yego_marketing_mensajes.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingMensajeCalendarioResponse {

    private Long id;
    private String titulo;
    private String modo;
    private List<String> diasActivos;
    private String horasEspecificas;
}

