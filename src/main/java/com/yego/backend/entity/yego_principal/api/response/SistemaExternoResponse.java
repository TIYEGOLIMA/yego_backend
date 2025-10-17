package com.yego.backend.entity.yego_principal.api.response;

import com.yego.backend.entity.yego_principal.entities.SistemaExterno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para response de sistema externo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SistemaExternoResponse {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private String url;
    private SistemaExterno.EstadoSistema estado;
    private LocalDateTime ultimoCheck;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
