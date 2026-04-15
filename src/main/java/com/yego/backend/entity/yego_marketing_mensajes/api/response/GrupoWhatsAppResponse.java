package com.yego.backend.entity.yego_marketing_mensajes.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrupoWhatsAppResponse {
    
    private String id;
    private String subject;
    private String pictureUrl;
}

