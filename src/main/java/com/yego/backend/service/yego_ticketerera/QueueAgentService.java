package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.AsignarModuloResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModulosEstadoResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.RecuperarModuloResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;

/**
 * Interface del servicio de QueueAgent del sistema YEGO Ticketerera
 */
public interface QueueAgentService {
    
    ResponseEntity<Map<String, Object>> liberarModuloDelUsuario(Long userId);
    
    ResponseEntity<Map<String, Object>> liberarModuloPorModuleId(Long moduleId);
    
    Optional<Long> obtenerQueueAgentIdPorUsuario(Long userId);
    
    Optional<RecuperarModuloResponse> recuperarModuloAsignado(Long userId);
    
    //giomar 2025-12-30
    ModulosEstadoResponse obtenerModulosDisponiblesYOcupados();
    
    //giomar 2025-12-30
    ResponseEntity<AsignarModuloResponse> asignarModuloAUsuario(Map<String, Object> request);
    
    ResponseEntity<Map<String, Object>> verificarJWT(Authentication authentication);
}
