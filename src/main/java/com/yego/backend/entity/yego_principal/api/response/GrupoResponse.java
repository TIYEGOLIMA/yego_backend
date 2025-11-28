package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoResponse {
    
    private Long id;
    private String nombre;
    private String icono;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}

