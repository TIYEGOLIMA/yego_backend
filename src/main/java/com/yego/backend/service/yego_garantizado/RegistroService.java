package com.yego.backend.service.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.request.RegistroRequest;
import com.yego.backend.entity.yego_garantizado.api.response.RegistroResponse;

/**
 * Servicio para operaciones relacionadas con registros de garantizado
 * Maneja la creación y gestión de registros de conductores
 * 
 * @author Sistema Yego
 * @version 1.0
 */
public interface RegistroService {
    
    /**
     * Crea un nuevo registro de garantizado
     * @param request datos del registro a crear
     * @return respuesta con el registro creado
     */
    RegistroResponse crearRegistro(RegistroRequest request);
}
