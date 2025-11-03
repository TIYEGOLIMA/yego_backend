package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Respuesta para el estado del botón de procesamiento
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoProcesoResponse {
    
    private Boolean bloqueado;
    
    private LocalDateTime ultimoProcesamiento;
    
    private String mensaje;
    
    private LocalDateTime proximoLunes;
}

