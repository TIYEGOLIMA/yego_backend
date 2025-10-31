package com.yego.backend.service.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.request.CrearCiudadRequest;
import com.yego.backend.entity.yego_garantizado.api.request.CrearPaisRequest;
import com.yego.backend.entity.yego_garantizado.api.response.PaisListResponse;
import com.yego.backend.entity.yego_garantizado.api.response.UbicacionResponse;
import com.yego.backend.entity.yego_garantizado.api.response.UbicacionesCompletasResponse;

import java.util.List;

/**
 * Interfaz del servicio de ubicaciones del sistema YEGO Garantizado
 */
public interface UbicacionService {
    
    /**
     * Crear un país
     * @param request Datos del país a crear
     * @return País creado
     */
    UbicacionResponse crearPais(CrearPaisRequest request);
    
    /**
     * Crear una ciudad
     * @param request Datos de la ciudad a crear
     * @return Ciudad creada
     */
    UbicacionResponse crearCiudad(CrearCiudadRequest request);
    
    /**
     * Obtener todos los países
     * @return Lista de países simplificada
     */
    List<PaisListResponse> obtenerPaises();
    
    /**
     * Obtener ciudades de un país
     * @param paisId ID del país
     * @return Lista de ciudades
     */
    List<UbicacionResponse> obtenerCiudades(Long paisId);
    
    /**
     * Obtener todas las ubicaciones completas (países con sus ciudades)
     * @return Respuesta con países y sus ciudades
     */
    UbicacionesCompletasResponse obtenerTodasLasUbicaciones();
}

