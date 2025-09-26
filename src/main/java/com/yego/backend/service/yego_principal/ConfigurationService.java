package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;

import java.util.List;

/**
 * Interfaz del servicio de configuraciones del sistema YEGO Principal
 */
public interface ConfigurationService {
    
    /**
     * Obtener todas las configuraciones
     */
    List<ConfigurationResponseDto> findAll();
    
    /**
     * Obtener configuraciones por categoría
     */
    List<ConfigurationResponseDto> findByCategory(String category);
    
    /**
     * Obtener configuración por clave
     */
    ConfigurationResponseDto findOne(String key);
    
    /**
     * Obtener valor de configuración
     */
    Object getValue(String key);
    
    /**
     * Obtener valor de configuración con valor por defecto
     */
    Object getValue(String key, Object defaultValue);
    
    /**
     * Establecer valor de configuración
     */
    ConfigurationResponseDto setValue(String key, Object value, String type);
    
    /**
     * Actualizar configuración
     */
    ConfigurationResponseDto update(String key, UpdateConfigurationDto updateConfigurationDto);
    
    /**
     * Eliminar configuración
     */
    void remove(String key);
    
    /**
     * Obtener categorías disponibles
     */
    List<String> getCategories();
    
    /**
     * Obtener configuración del sistema
     */
    SystemConfigDto getSystemConfig();
    
    /**
     * Inicializar configuraciones por defecto
     */
    void initializeDefaultConfigs();
    
    /**
     * Limpiar cache de configuraciones
     */
    void clearCache();
}