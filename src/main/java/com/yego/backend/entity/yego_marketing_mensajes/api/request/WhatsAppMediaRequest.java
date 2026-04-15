package com.yego.backend.entity.yego_marketing_mensajes.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMediaRequest {
    
    private String number;
    
    @JsonProperty("mediatype")
    private String mediatype; // image, document, video, audio
    
    private String caption;
    
    private String media; // URL en base64
    
    @JsonProperty("fileName")
    private String filename; // Nombre del archivo (requerido para base64) - La API espera "fileName" en camelCase
}

