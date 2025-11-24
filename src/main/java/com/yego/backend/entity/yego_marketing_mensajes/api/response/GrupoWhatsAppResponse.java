package com.yego.backend.entity.yego_marketing_mensajes.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response para grupos de WhatsApp
 * Contiene solo los campos necesarios: id, subject
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrupoWhatsAppResponse {
    
    private String id;
    private String subject;
    private String pictureUrl;
}

