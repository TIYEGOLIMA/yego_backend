package com.yego.backend.service.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.EstadoProcesoResponse;

/**
 * Servicio para gestionar el estado del procesamiento de garantizado
 */
public interface ProcesoGarantizadoEstadoService {
    
    /**
     * Obtener el estado actual del botón de procesamiento
     * @return EstadoProcesoResponse con información del bloqueo
     */
    EstadoProcesoResponse obtenerEstadoProceso();
    
    /**
     * Registrar que se ha procesado el garantizado
     * Esto bloquea el botón hasta el próximo lunes
     */
    void registrarProcesamiento();
}

