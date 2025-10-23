package com.yego.backend.service.yego_asistencia;

import java.util.List;

/**
 * Interfaz del servicio de mensajes del sistema YEGO Asistencia
 * Simplificado para mantener solo métodos esenciales
 */
public interface MessageService {
    
    // ===== MÉTODOS PRINCIPALES =====
    
    /**
     * Obtener mensaje aleatorio por tipo
     */
    String getRandomMessage(String messageType);
    
    /**
     * Obtener mensaje aleatorio de una lista específica
     */
    String getRandomMessage(List<String> messages);
    
    // ===== MÉTODOS POR TIPO DE MARCACIÓN =====
    
    /**
     * Obtener mensaje de entrada
     */
    String getEntryMessage();
    
    /**
     * Obtener mensaje de salida a refrigerio
     */
    String getExitBreakMessage();
    
    /**
     * Obtener mensaje de regreso de refrigerio
     */
    String getReturnBreakMessage();
    
    /**
     * Obtener mensaje de salida
     */
    String getExitMessage();
    
    /**
     * Obtener mensaje motivacional general
     */
    String getGeneralMessage();
    
    // ===== MÉTODOS DE UTILIDAD =====
    
    /**
     * Validar si un tipo de mensaje es válido
     */
    boolean isValidMessageType(String messageType);
    
    /**
     * Obtener mensaje de éxito
     */
    String getSuccessMessage(String actionType);
}

