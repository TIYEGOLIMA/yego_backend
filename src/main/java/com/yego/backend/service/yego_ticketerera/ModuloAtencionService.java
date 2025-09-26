package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;

import java.util.List;

/**
 * Interface del servicio de Módulos de Atención del sistema YEGO Ticketerera
 */
public interface ModuloAtencionService {
    
    List<ModuloAtencion> obtenerTodosLosModulosActivos();
    
    List<ModuloAtencion> obtenerTodosLosModulos();
    
    void cambiarEstadoModulo(Long moduleId, boolean activo);
    
    // Método simplificado para el controlador (sin lógica de negocio en el controlador)
    List<ModuloAtencionResponse> obtenerModulosParaFrontend();
}
