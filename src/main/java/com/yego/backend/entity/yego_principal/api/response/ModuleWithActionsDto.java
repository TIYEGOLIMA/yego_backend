package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa un módulo con sus acciones disponibles
 * Útil para formularios de creación/edición de roles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleWithActionsDto {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private String url;
    private String estado;
    private Boolean activo;
    private List<ActionDto> actions;
}

