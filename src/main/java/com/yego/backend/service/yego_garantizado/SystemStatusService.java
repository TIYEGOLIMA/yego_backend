package com.yego.backend.service.yego_garantizado;

/**
 * Servicio para gestión del estado del sistema
 * Maneja activación/desactivación automática y manual
 * 
 * @author Sistema Yego
 * @version 1.0
 */
public interface SystemStatusService {
    
    /**
     * Verifica si el sistema está activo
     * @return true si está activo, false si está desactivado
     */
    boolean isSystemActive();
    
    /**
     * Activa el sistema manualmente
     */
    void activateSystem();
    
    /**
     * Desactiva el sistema manualmente
     */
    void deactivateSystem();
    
    /**
     * Obtiene el estado actual del sistema como string
     * @return "ACTIVO" o "INACTIVO"
     */
    String getCurrentStatus();
    
    /**
     * Calcula la próxima hora de activación
     * @return String con la próxima activación
     */
    String getNextActivationTime();
    
    /**
     * Calcula la próxima hora de desactivación
     * @return String con la próxima desactivación
     */
    String getNextDeactivationTime();
}
