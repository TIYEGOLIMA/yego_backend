package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.QueueAgent;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_ticketerera.api.response.UserModuleStatusResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.RecuperarModuloResponse;
import com.yego.backend.repository.yego_ticketerera.QueueAgentRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de QueueAgent del sistema YEGO Ticketerera
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueAgentServiceImpl implements QueueAgentService {
    
    private final QueueAgentRepository queueAgentRepository;
    private final ModuloAtencionService moduloAtencionService;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public QueueAgent asignarModuloAUsuario(Long userId, Long moduleId) {
        log.info("Asignando módulo {} al usuario {}", moduleId, userId);
        
        // Verificar si el módulo ya está ocupado
        Optional<QueueAgent> moduloOcupado = queueAgentRepository.findByModuleIdAndIsActiveTrue(moduleId);
        if (moduloOcupado.isPresent() && "OCUPADO".equals(moduloOcupado.get().getStatus())) {
            throw new IllegalStateException("El módulo de atención ya está ocupado");
        }
        
        // Verificar si el usuario ya tiene un registro activo
        Optional<QueueAgent> usuarioExistente = queueAgentRepository.findByUserIdAndIsActiveTrue(userId);
        
        QueueAgent queueAgent;
        
        if (usuarioExistente.isPresent()) {
            // Usuario ya existe, actualizar su registro
            queueAgent = usuarioExistente.get();
            queueAgent.setModuleId(moduleId);
            queueAgent.setStatus("OCUPADO");
            queueAgent.setIsActive(true);
            queueAgent.setUpdatedAt(LocalDateTime.now());
            log.info("Actualizando registro existente para usuario {} - isActive: true", userId);
        } else {
            // Crear nueva asignación
            queueAgent = QueueAgent.builder()
                    .userId(userId)
                    .moduleId(moduleId)
                    .status("OCUPADO")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            log.info("Creando nuevo registro para usuario {} - isActive: true", userId);
        }
        
        QueueAgent savedAgent = queueAgentRepository.save(queueAgent);
        
        try {
            moduloAtencionService.cambiarEstadoModulo(moduleId, false);
            log.info("Módulo {} marcado como inactivo (is_active = false) - OCUPADO", moduleId);
        } catch (Exception e) {
            log.error("Error al desactivar módulo {}: {}", moduleId, e.getMessage());
        }
        
        log.info("Módulo {} asignado exitosamente al usuario {} - QueueAgent.isActive: true, yego_modules.is_active: false", moduleId, userId);
        
        return savedAgent;
    }
    
    @Override
    @Transactional
    public void liberarModuloDelUsuario(Long userId) {
        log.info("Liberando módulo del usuario {}", userId);
        
        Optional<QueueAgent> queueAgent = queueAgentRepository.findByUserIdAndIsActiveTrue(userId);
        if (queueAgent.isPresent()) {
            QueueAgent agent = queueAgent.get();
            Long moduleId = agent.getModuleId();
            
            // Cambiar el estado a LIBRE y desactivar el agente
            agent.setStatus("LIBRE");
            agent.setUpdatedAt(LocalDateTime.now());
            agent.setIsActive(false);
            
            queueAgentRepository.save(agent);
            
            // Reactivar el módulo de atención para que vuelva a estar disponible
            try {
                moduloAtencionService.cambiarEstadoModulo(moduleId, true);
                log.info("Módulo {} reactivado y disponible (is_active = true)", moduleId);
            } catch (Exception e) {
                log.error("Error al reactivar módulo {}: {}", moduleId, e.getMessage());
            }
            
            log.info("Módulo {} liberado del usuario {} - Estado: LIBRE, isActive: false", moduleId, userId);
        } else {
            log.warn("Usuario {} no tenía módulo asignado", userId);
        }
    }
    
    @Override
    @Transactional
    public void liberarModuloEspecifico(Long moduleId) {
        log.info("Liberando módulo específico {}", moduleId);
        
        Optional<QueueAgent> queueAgent = queueAgentRepository.findByModuleIdAndIsActiveTrue(moduleId);
        if (queueAgent.isPresent()) {
            QueueAgent agent = queueAgent.get();
            agent.setIsActive(false);
            agent.setStatus("LIBRE");
            agent.setUpdatedAt(LocalDateTime.now());
            
            queueAgentRepository.save(agent);
            
            // Reactivar el módulo de atención para que vuelva a estar disponible
            try {
                moduloAtencionService.cambiarEstadoModulo(moduleId, true);
                log.info("Módulo {} reactivado y disponible", moduleId);
            } catch (Exception e) {
                log.error("Error al reactivar módulo {}: {}", moduleId, e.getMessage());
            }
            
            log.info("Módulo {} liberado del usuario {}", moduleId, agent.getUserId());
        } else {
            log.warn("Módulo {} no estaba ocupado", moduleId);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QueueAgent> obtenerTodosLosAgentesActivos() {
        log.info("Obteniendo todos los agentes activos");
        return queueAgentRepository.findByIsActiveTrue();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<QueueAgent> obtenerAgentePorUsuario(Long userId) {
        log.info("Obteniendo agente para usuario {}", userId);
        return queueAgentRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Long> obtenerQueueAgentIdPorUsuario(Long userId) {
        log.info("Obteniendo queue_agent ID para usuario {}", userId);
        
        List<QueueAgent> agents = queueAgentRepository.findAllByUserIdAndIsActiveTrue(userId);
        
        if (agents.isEmpty()) {
            log.warn("Usuario {} no encontrado en queue_agents", userId);
            return Optional.empty();
        }
        
        if (agents.size() > 1) {
            log.warn("Usuario {} tiene {} registros activos en queue_agents. Usando el más reciente.", userId, agents.size());
            // Ordenar por fecha de creación descendente y tomar el más reciente
            agents.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }
        
        QueueAgent agent = agents.get(0);
        boolean estaOcupado = "OCUPADO".equals(agent.getStatus());
        
        if (estaOcupado) {
            log.info("QueueAgent ID {} encontrado para usuario {} con estado OCUPADO", agent.getId(), userId);
            return Optional.of(agent.getId());
        } else {
            log.warn("Usuario {} no está OCUPADO: estado={}, activo={}", userId, agent.getStatus(), agent.getIsActive());
            return Optional.empty();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<QueueAgent> verificarUsuarioConModuloOcupado(Long userId) {
        log.info("🎯 Verificando si usuario {} tiene módulo ocupado", userId);
        
        try {
            log.info("🎯 [DEBUG] Paso 1: Llamando al repositorio...");
            
            Optional<QueueAgent> queueAgent = queueAgentRepository.findByUserIdAndIsActiveTrue(userId);
            
            log.info("🎯 [DEBUG] Paso 2: Repositorio respondió, resultado: {}", queueAgent.isPresent() ? "presente" : "vacío");
            
            if (queueAgent.isPresent()) {
                QueueAgent agent = queueAgent.get();
                log.info("🎯 [DEBUG] Paso 3: QueueAgent encontrado: ID={}, ModuleId={}, Status={}, IsActive={}", 
                        agent.getId(), agent.getModuleId(), agent.getStatus(), agent.getIsActive());
                
                // Verificar si está ocupado
                if ("OCUPADO".equals(agent.getStatus()) && Boolean.TRUE.equals(agent.getIsActive())) {
                    log.info("✅ Usuario {} tiene módulo {} ocupado (ID: {})", userId, agent.getModuleId(), agent.getId());
                    return queueAgent;
                } else {
                    log.info("📭 Usuario {} tiene módulo pero NO está ocupado: status={}, isActive={}", 
                            userId, agent.getStatus(), agent.getIsActive());
                    return Optional.empty();
                }
            } else {
                log.info("📭 Usuario {} no tiene módulo asignado", userId);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("❌ Error verificando módulo ocupado del usuario {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error interno verificando módulo ocupado del usuario " + userId, e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserModuleStatusResponse verificarYRestaurarModuloUsuario(Long userId) {
        log.info("🎯 Verificando estado del módulo para usuario: {}", userId);
        
        try {
            // Verificar si el usuario tiene un módulo asignado activo
            Optional<QueueAgent> queueAgentOpt = queueAgentRepository.findByUserIdAndIsActiveTrue(userId);
            
            if (queueAgentOpt.isPresent()) {
                QueueAgent queueAgent = queueAgentOpt.get();
                
                log.info("✅ Usuario {} tiene módulo {} asignado (status: {})", userId, queueAgent.getModuleId(), queueAgent.getStatus());
                
                // Devolver solo los campos básicos del QueueAgent
                return UserModuleStatusResponse.builder()
                        .userId(queueAgent.getUserId())
                        .moduleId(queueAgent.getModuleId())
                        .status(queueAgent.getStatus())
                        .isActive(queueAgent.getIsActive())
                        .createdAt(queueAgent.getCreatedAt())
                        .updatedAt(queueAgent.getUpdatedAt())
                        .build();
                
            } else {
                log.info("📭 Usuario {} no tiene módulo asignado", userId);
                
                // Usuario sin módulo asignado
                return UserModuleStatusResponse.builder()
                        .userId(userId)
                        .moduleId(null)
                        .status("DISPONIBLE")
                        .isActive(false)
                        .createdAt(null)
                        .updatedAt(null)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("❌ Error verificando módulo del usuario {}: {}", userId, e.getMessage(), e);
            
            return UserModuleStatusResponse.builder()
                    .userId(userId)
                    .moduleId(null)
                    .status("ERROR")
                    .isActive(false)
                    .createdAt(null)
                    .updatedAt(null)
                    .build();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<RecuperarModuloResponse> recuperarModuloAsignado(Long userId) {
        log.info("🎯 Recuperando módulo asignado para usuario: {}", userId);
        
        try {
            // Buscar el módulo activo del usuario
            Optional<QueueAgent> queueAgentOpt = queueAgentRepository.findByUserIdAndIsActiveTrue(userId);
            
            if (queueAgentOpt.isPresent()) {
                QueueAgent queueAgent = queueAgentOpt.get();
                
                log.info("✅ Usuario {} tiene módulo {} asignado (status: {}, isActive: {})", 
                        userId, queueAgent.getModuleId(), queueAgent.getStatus(), queueAgent.getIsActive());
                
                RecuperarModuloResponse response = RecuperarModuloResponse.builder()
                        .moduleId(queueAgent.getModuleId())
                        .status(queueAgent.getStatus())
                        .isActive(queueAgent.getIsActive())
                        .createdAt(queueAgent.getCreatedAt())
                        .build();
                
                return Optional.of(response);
                
            } else {
                log.info("📭 Usuario {} no tiene módulo asignado", userId);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("❌ Error recuperando módulo asignado del usuario {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error interno recuperando módulo asignado del usuario " + userId, e);
        }
    }
    
    @Override
    public Long obtenerUserIdPorUsername(String username) {
        try {
            log.debug("🔍 [QueueAgentService] Buscando userId para username: {}", username);
            
            Optional<User> userOptional = userRepository.findByUsernameOrEmail(username, username);
            
            if (userOptional.isPresent()) {
                Long userId = userOptional.get().getId();
                log.debug("✅ [QueueAgentService] Usuario {} encontrado con ID: {}", username, userId);
                return userId;
            } else {
                log.warn("❌ [QueueAgentService] Usuario {} no encontrado en la base de datos", username);
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ [QueueAgentService] Error buscando usuario por username {}: {}", username, e.getMessage(), e);
            return null;
        }
    }
    
    // Métodos simplificados para el controlador (sin lógica de negocio en el controlador)
    
    @Override
    public org.springframework.http.ResponseEntity<QueueAgent> asignarModuloAUsuario(
            java.util.Map<String, Object> request, 
            org.springframework.security.core.Authentication authentication) {
        
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return org.springframework.http.ResponseEntity.status(401).build();
            }
            
            String username = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            
            Long requestingUserId = obtenerUserIdPorUsername(username);
            if (requestingUserId == null) {
                log.warn("❌ [QueueAgent] Usuario {} no encontrado", username);
                return org.springframework.http.ResponseEntity.status(401).build();
            }
            
            Long targetUserId = extraerLongDelRequest(request, "userId", "user_id");
            Long moduleId = extraerLongDelRequest(request, "moduleId", "module_id");
            
            if (targetUserId == null || moduleId == null) {
                log.warn("❌ [QueueAgent] Parámetros faltantes: userId={}, moduleId={}", targetUserId, moduleId);
                return org.springframework.http.ResponseEntity.badRequest().build();
            }
            
            // Verificar permisos
            if (!puedeAsignarModulo(requestingUserId, targetUserId, role)) {
                log.warn("❌ [QueueAgent] Usuario {} ({}) no tiene permisos para asignar módulo a usuario {}", 
                        username, role, targetUserId);
                return org.springframework.http.ResponseEntity.status(403).build();
            }
            
            QueueAgent queueAgent = asignarModuloAUsuario(targetUserId, moduleId);
            return org.springframework.http.ResponseEntity.ok(queueAgent);
            
        } catch (Exception e) {
            log.error("❌ [QueueAgent] Error asignando módulo: {}", e.getMessage(), e);
            return org.springframework.http.ResponseEntity.status(500).build();
        }
    }
    
    @Override
    public org.springframework.http.ResponseEntity<Void> liberarModuloDeUsuario(
            java.util.Map<String, Object> request, 
            org.springframework.security.core.Authentication authentication) {
        
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return org.springframework.http.ResponseEntity.status(401).build();
            }
            
            String username = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            
            Long requestingUserId = obtenerUserIdPorUsername(username);
            if (requestingUserId == null) {
                log.warn("❌ [QueueAgent] Usuario {} no encontrado", username);
                return org.springframework.http.ResponseEntity.status(401).build();
            }
            
            Long targetUserId = extraerLongDelRequest(request, "userId", "user_id");
            
            if (targetUserId == null) {
                log.warn("❌ [QueueAgent] Parámetro userId faltante");
                return org.springframework.http.ResponseEntity.badRequest().build();
            }
            
            // Verificar permisos
            if (!puedeLiberarModulo(requestingUserId, targetUserId, role)) {
                log.warn("❌ [QueueAgent] Usuario {} ({}) no tiene permisos para liberar módulo de usuario {}", 
                        username, role, targetUserId);
                return org.springframework.http.ResponseEntity.status(403).build();
            }
            
            liberarModuloDelUsuario(targetUserId);
            return org.springframework.http.ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("❌ [QueueAgent] Error liberando módulo: {}", e.getMessage(), e);
            return org.springframework.http.ResponseEntity.status(500).build();
        }
    }
    
    @Override
    public List<QueueAgent> obtenerAgentesActivos() {
        return obtenerTodosLosAgentesActivos();
    }
    
    @Override
    public UserModuleStatusResponse verificarEstadoModuloUsuario(Long userId) {
        return verificarYRestaurarModuloUsuario(userId);
    }
    
    @Override
    public RecuperarModuloResponse restaurarModuloUsuario(Long userId) {
        Optional<RecuperarModuloResponse> response = recuperarModuloAsignado(userId);
        return response.orElse(RecuperarModuloResponse.builder()
                .moduleId(null)
                .status("SIN_MODULO")
                .isActive(false)
                .createdAt(null)
                .build());
    }
    
    @Override
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> verificarJWT(
            org.springframework.security.core.Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return org.springframework.http.ResponseEntity.status(401)
                    .body(java.util.Map.of("error", "Token inválido o expirado"));
        }
        
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        
        Long userId = obtenerUserIdPorUsername(username);
        
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "valid", true,
                "username", username,
                "role", role,
                "userId", userId != null ? userId : "unknown"
        ));
    }
    
    // Métodos auxiliares para la lógica de permisos
    
    private Long extraerLongDelRequest(java.util.Map<String, Object> request, String... keys) {
        for (String key : keys) {
            Object value = request.get(key);
            if (value != null) {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                } else if (value instanceof String) {
                    try {
                        return Long.parseLong((String) value);
                    } catch (NumberFormatException e) {
                        log.warn("❌ No se pudo convertir '{}' a Long para key '{}'", value, key);
                    }
                }
            }
        }
        return null;
    }
    
    private boolean puedeAsignarModulo(Long requestingUserId, Long targetUserId, String role) {
        // SUPERADMIN y ADMIN pueden asignar a cualquiera
        if ("SUPERADMIN".equals(role) || "ADMIN".equals(role)) {
            return true;
        }
        
        // OPERADOR solo puede asignarse a sí mismo
        if ("OPERADOR".equals(role)) {
            return requestingUserId.equals(targetUserId);
        }
        
        return false;
    }
    
    private boolean puedeLiberarModulo(Long requestingUserId, Long targetUserId, String role) {
        // SUPERADMIN y ADMIN pueden liberar a cualquiera
        if ("SUPERADMIN".equals(role) || "ADMIN".equals(role)) {
            return true;
        }
        
        // OPERADOR solo puede liberarse a sí mismo
        if ("OPERADOR".equals(role)) {
            return requestingUserId.equals(targetUserId);
        }
        
        return false;
    }
}
