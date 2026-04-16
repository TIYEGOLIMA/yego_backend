package com.yego.backend.entity.yego_marketing_mensajes.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppTextRequest {

    private String number;
    private String text;
}
