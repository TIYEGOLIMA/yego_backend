package com.yego.backend.entity.yego_principal.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para valor de configuración del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationValueDto {
    
    private String key;
    private Object value;
}

