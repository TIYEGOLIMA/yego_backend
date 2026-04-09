package com.yego.backend.service.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;

import java.util.List;

/**
 * Interfaz del servicio de flotas del sistema YEGO Garantizado
 */
public interface FlotaService {
    
    /**
     * Obtener todas las flotas de Yego desde API externa
     * @return Lista de flotas filtradas
     */
    List<FlotaResponse> obtenerFlotas();

    /**
     * Todos los partners devueltos por la API externa (sin filtrar por lista de IDs).
     * Para resolución de nombres por {@code parkId} en otros módulos.
     */
    List<FlotaResponse> obtenerTodosLosPartners();
}
