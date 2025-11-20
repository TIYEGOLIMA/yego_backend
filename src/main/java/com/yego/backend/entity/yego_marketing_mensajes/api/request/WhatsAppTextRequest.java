package com.yego.backend.entity.yego_marketing_mensajes.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para enviar mensaje de texto a WhatsApp
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppTextRequest {
    
    private String number;
    private String text;
}

