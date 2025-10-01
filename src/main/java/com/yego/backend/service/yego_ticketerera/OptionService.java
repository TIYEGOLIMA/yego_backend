package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Option;

import java.util.List;

/**
 * Interface del servicio de Opciones del sistema YEGO Ticketerera
 */
public interface OptionService {
    
    /**
     * Obtiene todas las opciones activas ordenadas por prioridad
     * @return Lista de todas las opciones activas
     */
    List<Option> obtenerTodasLasOpciones();
    
    /**
     * Obtiene solo los módulos principales (opciones sin padre) activos
     * @return Lista de opciones principales activas
     */
    List<Option> obtenerModulosActivos();
    
    /**
     * Obtiene las subopciones de un módulo específico
     * @param parentId ID del módulo padre
     * @return Lista de subopciones del módulo
     */
    List<Option> obtenerSubopciones(Long parentId);
}

