package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta del cambio de estado activo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleActivoResponse {
    
    private Boolean success;
    private String message;
    private SistemaExternoData data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SistemaExternoData {
        private Long id;
        private String nombre;
        private Boolean activo;
        private String url;
    }
}
