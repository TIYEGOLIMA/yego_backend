package com.yego.backend.entity.yego_marketing_mensajes.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para enviar mensaje con media a WhatsApp
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMediaRequest {
    
    private String number;
    private String mediatype; // image, pdf, video, audio
    private String caption;
    private String media; // URL en base64
}

