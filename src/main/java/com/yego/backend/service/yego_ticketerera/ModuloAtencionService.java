package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloUsuarioResponse;

import java.util.List;

/**
 * Interface del servicio de Módulos de Atención del sistema YEGO Ticketerera
 */
public interface ModuloAtencionService {
    
    //giomar 2025-12-30
    List<ModuloAtencionResponse> obtenerTodosLosModulosActivosResponse();
    
    void cambiarEstadoModulo(Long moduleId, boolean activo);
    
    ModuloUsuarioResponse verificarModuloOListarDisponibles(Long userId);
}
