package com.yego.backend.entity.yego_principal.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cambiar el estado activo de un sistema externo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleActivoRequest {
    
    private Boolean activo;
}
