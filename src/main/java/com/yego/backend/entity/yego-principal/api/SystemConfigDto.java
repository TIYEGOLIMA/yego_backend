package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para configuración del sistema organizada por categorías YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigDto {
    
    private Map<String, Object> system;
    private Map<String, Object> security;
    private Map<String, Object> ui;
    private Map<String, Object> audit;
    private Map<String, Object> imports;
}
