package com.yego.backend.service.yego_asistencia;

import com.yego.backend.entity.yego_principal.entities.User;

import java.util.Map;
import java.util.Optional;

/**
 * Interfaz del servicio de validación de tokens del sistema YEGO Asistencia
 * Solo maneja validación de tokens, no autenticación (eso se maneja en yego_principal)
 */
public interface TokenValidationService {
    
    /**
     * Validar token JWT
     */
    boolean validateToken(String token);
    
    /**
     * Obtener usuario por token
     */
    User getUserByToken(String token);
    
    /**
     * Obtener usuario desde token
     */
    Optional<User> getUserFromToken(String token);
    
    /**
     * Obtener claims del token
     */
    Map<String, Object> getTokenClaims(String token);
    
    /**
     * Obtener roles del usuario
     */
    String[] getUserRoles(String token);
    
    /**
     * Verificar si el usuario tiene un rol específico
     */
    boolean hasRole(String username, String role);
    
    /**
     * Validar si el usuario está autenticado
     */
    boolean isAuthenticated(String token);
}
