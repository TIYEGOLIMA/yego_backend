package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.QueueAgent;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_ticketerera.api.response.AsignarModuloResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloOcupadoResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModulosEstadoResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.RecuperarModuloResponse;
import com.yego.backend.repository.yego_ticketerera.QueueAgentRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import com.yego.backend.handler.yego_ticketerera.TicketNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de QueueAgent del sistema YEGO Ticketerera
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueAgentServiceImpl implements QueueAgentService {
    
    private final QueueAgentRepository queueAgentRepository;
    private final UserRepository userRepository;
    private final ModuloAtencionService moduloAtencionService;
    private final TicketNotificationHandler ticketNotificationHandler;
    
    // ========== MÉTODOS DE NEGOCIO PRINCIPALES ==========
    //giomar 2025-12-30
    @Override
    @Transactional
    public ResponseEntity<Map<String, Object>> liberarModuloDelUsuario(Long userId) {
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
            moduloAtencionService.cambiarEstadoModulo(moduleId, false);
            log.info("Módulo {} reactivado y disponible (is_active = false)", moduleId);
            
            // Enviar lista actualizada de módulos por WebSocket
            ModulosEstadoResponse modulosEstado = obtenerModulosDisponiblesYOcupados();
            ticketNotificationHandler.enviarModulosActualizados(modulosEstado);
            
            log.info("Módulo {} liberado del usuario {} - Estado: LIBRE, isActive: false", moduleId, userId);
            return ResponseEntity.ok(Map.of("message", "Módulo liberado exitosamente", "moduleId", moduleId, "userId", userId));
        } else {
            log.warn("Usuario {} no tenía módulo asignado", userId);
            return ResponseEntity.ok(Map.of("message", "Usuario no tenía módulo asignado", "userId", userId));
        }
    }
    
    //giomar 2025-12-30
    @Override
    @Transactional
    public ResponseEntity<Map<String, Object>> liberarModuloPorModuleId(Long moduleId) {
        log.info("Liberando módulo {}", moduleId);
        
        Optional<QueueAgent> queueAgent = queueAgentRepository.findByModuleIdAndIsActiveTrue(moduleId);
        if (queueAgent.isPresent() && "OCUPADO".equals(queueAgent.get().getStatus())) {
            QueueAgent agent = queueAgent.get();
            Long userId = agent.getUserId();
            
            // Cambiar el estado a LIBRE y desactivar el agente
            agent.setStatus("LIBRE");
            agent.setUpdatedAt(LocalDateTime.now());
            agent.setIsActive(false);
            
            queueAgentRepository.save(agent);
            moduloAtencionService.cambiarEstadoModulo(moduleId, false);
            log.info("Módulo {} reactivado y disponible (is_active = false)", moduleId);
            
            // Enviar lista actualizada de módulos por WebSocket
            ModulosEstadoResponse modulosEstado = obtenerModulosDisponiblesYOcupados();
            ticketNotificationHandler.enviarModulosActualizados(modulosEstado);
            
            log.info("Módulo {} liberado del usuario {} - Estado: LIBRE, isActive: false", moduleId, userId);
            return ResponseEntity.ok(Map.of("message", "Módulo liberado exitosamente", "moduleId", moduleId, "userId", userId));
        } else {
            log.warn("Módulo {} no está ocupado o no tiene agente asignado", moduleId);
            return ResponseEntity.ok(Map.of("message", "Módulo no está ocupado", "moduleId", moduleId));
        }
    }
    
    // ========== MÉTODOS DE CONSULTA ==========
    
    //giomar 2025-12-30
    @Override
    @Transactional(readOnly = true)
    public ModulosEstadoResponse obtenerModulosDisponiblesYOcupados() {
        log.info("Obteniendo módulos disponibles y ocupados");
        
        // Obtener módulos disponibles (isActive = false)
        List<ModuloAtencionResponse> modulosDisponibles = moduloAtencionService.obtenerTodosLosModulosActivosResponse();
        
        // Obtener módulos ocupados (status OCUPADO y isActive = true)
        List<QueueAgent> agentesOcupados = queueAgentRepository.findByStatusAndIsActiveTrue("OCUPADO");
        
        // Obtener todos los usuarios de una vez para evitar N+1 queries
        List<Long> userIds = agentesOcupados.stream()
                .map(QueueAgent::getUserId)
                .distinct()
                .toList();
        
        Map<Long, String> usuariosMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                    User::getId,
                    user -> (user.getName() + " " + user.getLastName()).trim()
                ));
        
        List<ModuloOcupadoResponse> modulosOcupados = agentesOcupados.stream()
                .map(agente -> ModuloOcupadoResponse.builder()
                        .moduleId(agente.getModuleId())
                        .userId(agente.getUserId())
                        .userName(usuariosMap.getOrDefault(agente.getUserId(), "Usuario " + agente.getUserId()))
                        .status(agente.getStatus())
                        .horaAsignacion(agente.getCreatedAt())
                        .updatedAt(agente.getUpdatedAt())
                        .build())
                .toList();
        
        log.info("Módulos disponibles: {}, Módulos ocupados: {}", modulosDisponibles.size(), modulosOcupados.size());
        
        return ModulosEstadoResponse.builder()
                .modulosDisponibles(modulosDisponibles)
                .modulosOcupados(modulosOcupados)
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Long> obtenerQueueAgentIdPorUsuario(Long userId) {
        log.info("Obteniendo queue_agent ID para usuario {}", userId);
        
        return queueAgentRepository.findByUserIdAndIsActiveTrue(userId)
                .filter(agent -> "OCUPADO".equals(agent.getStatus()))
                .map(agent -> {
                    log.info("QueueAgent ID {} encontrado para usuario {} con estado OCUPADO", agent.getId(), userId);
                    return agent.getId();
                });
    }
    
    // GIOMAR 2025-12-30
    @Override
    @Transactional(readOnly = true)
    public Optional<RecuperarModuloResponse> recuperarModuloAsignado(Long userId) {
        log.info("Recuperando módulo asignado para usuario: {}", userId);
        
        return queueAgentRepository.findByUserIdAndIsActiveTrue(userId)
                .map(queueAgent -> {
                    log.info("Usuario {} tiene módulo {} asignado (status: {}, isActive: {})", 
                            userId, queueAgent.getModuleId(), queueAgent.getStatus(), queueAgent.getIsActive());
                    
                    return RecuperarModuloResponse.builder()
                            .moduleId(queueAgent.getModuleId())
                            .status(queueAgent.getStatus())
                            .isActive(queueAgent.getIsActive())
                            .createdAt(queueAgent.getCreatedAt())
                            .build();
                });
    }
    
    // ========== MÉTODOS PARA EL CONTROLADOR ==========
    
    //giomar 2025-12-30
    @Override
    @Transactional
    public ResponseEntity<AsignarModuloResponse> asignarModuloAUsuario(Map<String, Object> request) {
        
        try {
            // Extraer parámetros del request
            Long userId = ((Number) request.get("userId")).longValue();
            Long moduleId = ((Number) request.get("moduleId")).longValue();
            
            log.info("Asignando módulo {} al usuario {}", moduleId, userId);
            
            // Verificar si el módulo ya está ocupado
            Optional<QueueAgent> moduloOcupado = queueAgentRepository.findByModuleIdAndIsActiveTrue(moduleId);
            if (moduloOcupado.isPresent() && "OCUPADO".equals(moduloOcupado.get().getStatus())) {
                log.warn("⚠️ [QueueAgent] El módulo de atención ya está ocupado");
                return ResponseEntity.status(409).build();
            }
            
            // Obtener o crear registro para el usuario
            QueueAgent queueAgent = queueAgentRepository.findByUserIdAndIsActiveTrue(userId)
                    .map(existente -> {
                        // Actualizar registro existente
                        existente.setModuleId(moduleId);
                        existente.setUpdatedAt(LocalDateTime.now());
                        log.info("Actualizando registro existente para usuario {}", userId);
                        return existente;
                    })
                    .orElseGet(() -> {
                        // Crear nueva asignación
                        log.info("Creando nuevo registro para usuario {}", userId);
                        return QueueAgent.builder()
                                .userId(userId)
                                .moduleId(moduleId)
                                .status("OCUPADO")
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .build();
                    });
            
            queueAgentRepository.save(queueAgent);
            
            moduloAtencionService.cambiarEstadoModulo(moduleId, true);
            log.info("Módulo {} marcado como ocupado (is_active = true)", moduleId);
            
            // Enviar lista actualizada de módulos por WebSocket
            ModulosEstadoResponse modulosEstado = obtenerModulosDisponiblesYOcupados();
            ticketNotificationHandler.enviarModulosActualizados(modulosEstado);
            
            log.info("Módulo {} asignado exitosamente al usuario {}", moduleId, userId);
            
            // Construir y devolver respuesta
            AsignarModuloResponse response = AsignarModuloResponse.builder()
                    .userId(userId)
                    .moduleId(moduleId)
                    .status("OCUPADO")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [QueueAgent] Argumento inválido: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ [QueueAgent] Error asignando módulo: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
    
    @Override
    public ResponseEntity<Map<String, Object>> verificarJWT(
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token inválido o expirado"));
        }
        
        String userIdString = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        
        Long userId;
        try {
            userId = Long.parseLong(userIdString);
            log.debug("✅ [QueueAgent] Usuario autenticado con ID: {}", userId);
        } catch (NumberFormatException e) {
            log.warn("❌ [QueueAgent] ID de usuario inválido: {}", userIdString);
            return ResponseEntity.status(401).build();
        }
        
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "userId", userId,
                "role", role
        ));
    }
}
