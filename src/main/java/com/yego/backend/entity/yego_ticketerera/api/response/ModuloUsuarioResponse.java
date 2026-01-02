package com.yego.backend.entity.yego_ticketerera.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de response para verificar módulo asignado o listar módulos disponibles
 * en el sistema YEGO Ticketerera
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModuloUsuarioResponse {
    
    private Boolean tieneModuloAsignado;
    private RecuperarModuloResponse moduloAsignado;
    private List<ModuloAtencionResponse> modulosDisponibles;
}

