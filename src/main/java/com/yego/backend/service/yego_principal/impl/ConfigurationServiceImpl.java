package com.yego.backend.service.yego_principal.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.Configuration;
import com.yego.backend.repository.yego_principal.ConfigurationRepository;
import com.yego.backend.service.yego_principal.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de configuraciones del sistema YEGO Principal
 * Equivalente a ConfigurationService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationServiceImpl implements ConfigurationService {
    
    private final ConfigurationRepository configurationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    
    @Override
    public List<ConfigurationResponseDto> findAll() {
        List<Configuration> configurations = configurationRepository.findAllByOrderByCategoryAscKeyAsc();
        
        return configurations.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ConfigurationResponseDto> findByCategory(String category) {
        List<Configuration> configurations = configurationRepository.findByCategoryOrderByKeyAsc(category);
        
        return configurations.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public ConfigurationResponseDto findOne(String key) {
        Configuration configuration = configurationRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Configuración con clave '" + key + "' no encontrada"));
        
        return mapToResponseDto(configuration);
    }
    
    @Override
    public Object getValue(String key) {
        return getValue(key, null);
    }
    
    @Override
    public Object getValue(String key, Object defaultValue) {
        // Verificar cache primero
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        
        try {
            Configuration configuration = configurationRepository.findByKey(key)
                    .orElseThrow(() -> new EntityNotFoundException("Configuración con clave '" + key + "' no encontrada"));
            
            Object value = parseValue(configuration.getValue(), configuration.getType());
            
            // Guardar en cache
            cache.put(key, value);
            
            return value;
            
        } catch (EntityNotFoundException e) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw e;
        }
    }
    
    @Override
    @Transactional
    public ConfigurationResponseDto setValue(String key, Object value, String type) {
        Optional<Configuration> existingConfig = configurationRepository.findByKey(key);
        
        Configuration configuration;
        if (existingConfig.isPresent()) {
            configuration = existingConfig.get();
            configuration.setValue(stringifyValue(value, type));
            configuration.setType(type);
        } else {
            configuration = Configuration.builder()
                    .key(key)
                    .value(stringifyValue(value, type))
                    .type(type)
                    .build();
        }
        
        Configuration savedConfiguration = configurationRepository.save(configuration);
        
        // Actualizar cache
        cache.put(key, value);
        
        log.info("⚙️ Configuración YEGO Principal actualizada: {} = {}", key, value);
        
        return mapToResponseDto(savedConfiguration);
    }
    
    @Override
    @Transactional
    public ConfigurationResponseDto update(String key, UpdateConfigurationDto updateConfigurationDto) {
        Configuration configuration = configurationRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Configuración con clave '" + key + "' no encontrada"));
        
        // Actualizar campos
        if (updateConfigurationDto.getValue() != null) {
            configuration.setValue(updateConfigurationDto.getValue());
        }
        if (updateConfigurationDto.getDescription() != null) {
            configuration.setDescription(updateConfigurationDto.getDescription());
        }
        if (updateConfigurationDto.getCategory() != null) {
            configuration.setCategory(updateConfigurationDto.getCategory());
        }
        if (updateConfigurationDto.getType() != null) {
            configuration.setType(updateConfigurationDto.getType());
        }
        
        Configuration savedConfiguration = configurationRepository.save(configuration);
        
        // Limpiar cache para esta clave
        cache.remove(key);
        
        log.info("⚙️ Configuración YEGO Principal actualizada: {}", key);
        
        return mapToResponseDto(savedConfiguration);
    }
    
    @Override
    @Transactional
    public void remove(String key) {
        Configuration configuration = configurationRepository.findByKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Configuración con clave '" + key + "' no encontrada"));
        
        configurationRepository.delete(configuration);
        
        // Limpiar cache
        cache.remove(key);
        
        log.info("🗑️ Configuración YEGO Principal eliminada: {}", key);
    }
    
    @Override
    public List<String> getCategories() {
        return configurationRepository.findDistinctCategories();
    }
    
    @Override
    public SystemConfigDto getSystemConfig() {
        List<Configuration> configurations = configurationRepository.findAllByOrderByCategoryAscKeyAsc();
        
        Map<String, Object> system = new HashMap<>();
        Map<String, Object> security = new HashMap<>();
        Map<String, Object> ui = new HashMap<>();
        Map<String, Object> audit = new HashMap<>();
        Map<String, Object> imports = new HashMap<>();
        
        for (Configuration config : configurations) {
            Object value = parseValue(config.getValue(), config.getType());
            
            if (config.getCategory() != null) {
                switch (config.getCategory().toLowerCase()) {
                    case "security":
                        security.put(config.getKey(), value);
                        break;
                    case "ui":
                        ui.put(config.getKey(), value);
                        break;
                    case "audit":
                        audit.put(config.getKey(), value);
                        break;
                    case "imports":
                        imports.put(config.getKey(), value);
                        break;
                    default:
                        system.put(config.getKey(), value);
                        break;
                }
            } else {
                system.put(config.getKey(), value);
            }
        }
        
        return SystemConfigDto.builder()
                .system(system)
                .security(security)
                .ui(ui)
                .audit(audit)
                .imports(imports)
                .build();
    }
    
    @Override
    @Transactional
    public void initializeDefaultConfigs() {
        List<DefaultConfigData> defaultConfigs = getDefaultConfigsData();
        
        for (DefaultConfigData defaultConfig : defaultConfigs) {
            if (!configurationRepository.existsByKey(defaultConfig.key)) {
                Configuration configuration = Configuration.builder()
                        .key(defaultConfig.key)
                        .value(defaultConfig.value)
                        .description(defaultConfig.description)
                        .category(defaultConfig.category)
                        .type(defaultConfig.type)
                        .build();
                
                configurationRepository.save(configuration);
                log.info("⚙️ Configuración por defecto YEGO Principal creada: {}", defaultConfig.key);
            }
        }
    }
    
    @Override
    public void clearCache() {
        cache.clear();
        log.info("🧹 Cache de configuraciones YEGO Principal limpiado");
    }
    
    private Object parseValue(String value, String type) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        switch (type.toLowerCase()) {
            case "number":
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return value;
                }
            case "boolean":
                return Boolean.parseBoolean(value);
            case "json":
                try {
                    return objectMapper.readValue(value, Object.class);
                } catch (JsonProcessingException e) {
                    return value;
                }
            default:
                return value;
        }
    }
    
    private String stringifyValue(Object value, String type) {
        if (value == null) {
            return "";
        }
        
        switch (type.toLowerCase()) {
            case "json":
                try {
                    return objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    return String.valueOf(value);
                }
            default:
                return String.valueOf(value);
        }
    }
    
    private ConfigurationResponseDto mapToResponseDto(Configuration configuration) {
        return ConfigurationResponseDto.builder()
                .id(configuration.getId())
                .key(configuration.getKey())
                .value(configuration.getValue())
                .description(configuration.getDescription())
                .category(configuration.getCategory())
                .type(configuration.getType())
                .createdAt(configuration.getCreatedAt())
                .updatedAt(configuration.getUpdatedAt())
                .build();
    }
    
    private List<DefaultConfigData> getDefaultConfigsData() {
        return Arrays.asList(
                new DefaultConfigData("system_name", "Yego Integral", "Nombre del sistema", "system", "string"),
                new DefaultConfigData("system_version", "1.0.0", "Versión del sistema", "system", "string"),
                new DefaultConfigData("session_timeout", "3600", "Tiempo de sesión en segundos", "security", "number"),
                new DefaultConfigData("max_login_attempts", "5", "Máximo intentos de login", "security", "number"),
                new DefaultConfigData("password_min_length", "8", "Longitud mínima de contraseña", "security", "number"),
                new DefaultConfigData("enable_audit_logs", "true", "Habilitar logs de auditoría", "audit", "boolean"),
                new DefaultConfigData("default_language", "es", "Idioma por defecto", "system", "string"),
                new DefaultConfigData("theme_default", "light", "Tema por defecto", "ui", "string")
        );
    }
    
    private static class DefaultConfigData {
        final String key;
        final String value;
        final String description;
        final String category;
        final String type;
        
        DefaultConfigData(String key, String value, String description, String category, String type) {
            this.key = key;
            this.value = value;
            this.description = description;
            this.category = category;
            this.type = type;
        }
    }
}

