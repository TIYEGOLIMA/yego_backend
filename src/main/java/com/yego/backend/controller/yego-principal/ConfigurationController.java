package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.service.yego_principal.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para configuraciones del sistema YEGO Principal
 * Equivalente a ConfigurationController de NestJS
 */
@Slf4j
@RestController
@RequestMapping("/api/yego-principal/configuration")
@RequiredArgsConstructor
public class ConfigurationController {
    
    private final ConfigurationService configurationService;
    
    /**
     * Obtener todas las configuraciones
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findAll() {
        try {
            List<ConfigurationResponseDto> configurations = configurationService.findAll();
            return ResponseEntity.ok(configurations);
            
        } catch (Exception e) {
            log.error("Error obteniendo configuraciones YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener configuración del sistema organizada por categorías
     */
    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSystemConfig() {
        try {
            SystemConfigDto systemConfig = configurationService.getSystemConfig();
            return ResponseEntity.ok(systemConfig);
            
        } catch (Exception e) {
            log.error("Error obteniendo configuración del sistema YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener categorías de configuración
     */
    @GetMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCategories() {
        try {
            List<String> categories = configurationService.getCategories();
            return ResponseEntity.ok(categories);
            
        } catch (Exception e) {
            log.error("Error obteniendo categorías YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener configuraciones por categoría
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findByCategory(@PathVariable String category) {
        try {
            List<ConfigurationResponseDto> configurations = configurationService.findByCategory(category);
            return ResponseEntity.ok(configurations);
            
        } catch (Exception e) {
            log.error("Error obteniendo configuraciones por categoría YEGO Principal {}: {}", category, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener configuración específica por clave
     */
    @GetMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findOne(@PathVariable String key) {
        try {
            ConfigurationResponseDto configuration = configurationService.findOne(key);
            return ResponseEntity.ok(configuration);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo configuración YEGO Principal {}: {}", key, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener valor de configuración específica
     */
    @GetMapping("/{key}/value")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getValue(@PathVariable String key) {
        try {
            Object value = configurationService.getValue(key);
            ConfigurationValueDto response = ConfigurationValueDto.builder()
                    .key(key)
                    .value(value)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo valor de configuración YEGO Principal {}: {}", key, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Actualizar configuración específica
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable String key, 
                                   @Valid @RequestBody UpdateConfigurationDto updateConfigurationDto) {
        try {
            ConfigurationResponseDto configuration = configurationService.update(key, updateConfigurationDto);
            
            log.info("⚙️ Configuración YEGO Principal actualizada: {}", key);
            
            return ResponseEntity.ok(configuration);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error actualizando configuración YEGO Principal {}: {}", key, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Establecer valor de configuración
     */
    @PostMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setValue(@PathVariable String key, 
                                     @Valid @RequestBody SetValueDto setValueDto) {
        try {
            ConfigurationResponseDto configuration = configurationService.setValue(
                    key, setValueDto.getValue(), setValueDto.getType());
            
            log.info("⚙️ Valor de configuración YEGO Principal establecido: {} = {}", key, setValueDto.getValue());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(configuration);
            
        } catch (Exception e) {
            log.error("Error estableciendo valor de configuración YEGO Principal {}: {}", key, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Eliminar configuración específica
     */
    @DeleteMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> remove(@PathVariable String key) {
        try {
            configurationService.remove(key);
            
            log.info("🗑️ Configuración YEGO Principal eliminada: {}", key);
            
            return ResponseEntity.ok(Map.of("message", "Configuración '" + key + "' eliminada exitosamente"));
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error eliminando configuración YEGO Principal {}: {}", key, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Inicializar configuraciones por defecto
     */
    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> initializeDefaultConfigs() {
        try {
            configurationService.initializeDefaultConfigs();
            
            log.info("⚙️ Configuraciones por defecto YEGO Principal inicializadas");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Configuraciones por defecto inicializadas exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error inicializando configuraciones por defecto YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Limpiar cache de configuraciones
     */
    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> clearCache() {
        try {
            configurationService.clearCache();
            
            log.info("🧹 Cache de configuraciones YEGO Principal limpiado");
            
            return ResponseEntity.ok(Map.of("message", "Cache de configuraciones limpiado exitosamente"));
            
        } catch (Exception e) {
            log.error("Error limpiando cache de configuraciones YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
}
