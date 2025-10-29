package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controlador REST para configuraciones del sistema YEGO Principal
 */
@Slf4j
@RestController
@RequestMapping("/api/configurations")
@RequiredArgsConstructor
public class ConfigurationController {
    
    private final ConfigurationService configurationService;
    
    /**
     * Obtener todas las configuraciones
     */
    @GetMapping
    public ResponseEntity<List<ConfigurationResponseDto>> findAll() {
        List<ConfigurationResponseDto> configurations = configurationService.findAll();
        return ResponseEntity.ok(configurations);
    }
    
    /**
     * Obtener configuraciones por categoría
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ConfigurationResponseDto>> findByCategory(@PathVariable String category) {
        List<ConfigurationResponseDto> configurations = configurationService.findByCategory(category);
        return ResponseEntity.ok(configurations);
    }
    
    /**
     * Obtener configuración por clave
     */
    @GetMapping("/{key}")
    public ResponseEntity<?> findByKey(@PathVariable String key) {
        ConfigurationResponseDto configuration = configurationService.findOne(key);
        return ResponseEntity.ok(configuration);
    }
    
    /**
     * Actualizar configuración
     */
    @PutMapping("/{key}")
    public ResponseEntity<?> update(@PathVariable String key, 
                                   @Valid @RequestBody UpdateConfigurationDto updateConfigurationDto) {
        ConfigurationResponseDto configuration = configurationService.update(key, updateConfigurationDto);
        return ResponseEntity.ok(configuration);
    }
    
    /**
     * Eliminar configuración
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<?> remove(@PathVariable String key) {
        configurationService.remove(key);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Obtener configuración del sistema
     */
    @GetMapping("/system")
    public ResponseEntity<?> getSystemConfig() {
        SystemConfigDto config = configurationService.getSystemConfig();
        return ResponseEntity.ok(config);
    }
}