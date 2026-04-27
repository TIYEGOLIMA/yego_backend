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
public class ModuleResponse {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private String url;
    private String codigo;
    private String estado;
    private String icono;
    private GrupoResponse grupo;
    private LocalDateTime ultimoCheck;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
