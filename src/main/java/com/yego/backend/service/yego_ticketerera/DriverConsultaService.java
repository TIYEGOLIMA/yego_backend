package com.yego.backend.service.yego_ticketerera;

import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

/**
 * Interface del servicio de consulta de conductores del sistema YEGO Ticketerera
 */
public interface DriverConsultaService {
    
    Optional<Map<String, Object>> buscarPorTelefono(String telefono);
    
    Map<String, Object> consultarYRegistrarPorDni(String dni, String phone);
    
    Map<String, Object> registrarNuevoConductor(String firstName, String lastName, String phone);
    
    void limpiarCache();
    
    // Métodos simplificados para el controlador (sin lógica de negocio en el controlador)
    ResponseEntity<Map<String, Object>> buscarConductorConRespuestaCompleta(String phoneDigits);
    
    ResponseEntity<Map<String, Object>> registrarConductorManualConRespuesta(Map<String, String> datos);
    
    ResponseEntity<Map<String, Object>> registrarConductorPorDniConRespuesta(Map<String, String> datos);
    
    ResponseEntity<Map<String, Object>> limpiarCacheConRespuesta();
}
