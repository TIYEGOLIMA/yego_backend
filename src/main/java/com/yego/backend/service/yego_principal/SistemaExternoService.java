package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.SistemaExternoRequest;
import com.yego.backend.entity.yego_principal.api.response.SistemaExternoResponse;
import com.yego.backend.entity.yego_principal.entities.SistemaExterno;

import java.util.List;

/**
 * Interfaz del servicio de sistemas externos
 */
public interface SistemaExternoService {
    
    /**
     * Obtener todos los sistemas externos
     */
    List<SistemaExternoResponse> obtenerTodos();
    
    /**
     * Obtener sistema externo por ID
     */
    SistemaExternoResponse obtenerPorId(Long id);
    
    /**
     * Crear nuevo sistema externo
     */
    SistemaExternoResponse crear(SistemaExternoRequest request);
    
    /**
     * Actualizar sistema externo
     */
    SistemaExternoResponse actualizar(Long id, SistemaExternoRequest request);
    
    /**
     * Cambiar estado de un sistema externo
     */
    SistemaExternoResponse cambiarEstado(Long id, SistemaExterno.EstadoSistema nuevoEstado);
    
    /**
     * Verificar estado de un sistema externo
     */
    SistemaExternoResponse verificarEstado(Long id);
    
    /**
     * Eliminar sistema externo
     */
    void eliminar(Long id);
    
    /**
     * Buscar sistemas externos por término
     */
    List<SistemaExternoResponse> buscarPorTermino(String termino);
    
    /**
     * Cambiar estado activo de un sistema externo
     */
    SistemaExternoResponse toggleActivo(Long id, Boolean activo);
}
