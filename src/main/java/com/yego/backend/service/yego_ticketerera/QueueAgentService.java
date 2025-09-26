package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueAgent;
import com.yego.backend.entity.yego_ticketerera.api.response.UserModuleStatusResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.RecuperarModuloResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface del servicio de QueueAgent del sistema YEGO Ticketerera
 */
public interface QueueAgentService {
    
    QueueAgent asignarModuloAUsuario(Long userId, Long moduleId);
    
    void liberarModuloDelUsuario(Long userId);
    
    void liberarModuloEspecifico(Long moduleId);
    
    List<QueueAgent> obtenerTodosLosAgentesActivos();
    
    Optional<QueueAgent> obtenerAgentePorUsuario(Long userId);
    
    Optional<Long> obtenerQueueAgentIdPorUsuario(Long userId);
    
    Optional<QueueAgent> verificarUsuarioConModuloOcupado(Long userId);
    
    UserModuleStatusResponse verificarYRestaurarModuloUsuario(Long userId);
    
    Optional<RecuperarModuloResponse> recuperarModuloAsignado(Long userId);
    
    Long obtenerUserIdPorUsername(String username);
    
    // Métodos simplificados para el controlador (sin lógica de negocio en el controlador)
    ResponseEntity<QueueAgent> asignarModuloAUsuario(Map<String, Object> request, Authentication authentication);
    
    ResponseEntity<Void> liberarModuloDeUsuario(Map<String, Object> request, Authentication authentication);
    
    List<QueueAgent> obtenerAgentesActivos();
    
    UserModuleStatusResponse verificarEstadoModuloUsuario(Long userId);
    
    RecuperarModuloResponse restaurarModuloUsuario(Long userId);
    
    ResponseEntity<Map<String, Object>> verificarJWT(Authentication authentication);
}
