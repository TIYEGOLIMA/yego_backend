package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import com.yego.backend.entity.yego_ticketerera.entities.QueueAgent;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_ticketerera.api.response.AsignarModuloResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloOcupadoResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModulosEstadoResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.RecuperarModuloResponse;
import com.yego.backend.repository.yego_ticketerera.ModuloAtencionRepository;
import com.yego.backend.repository.yego_ticketerera.QueueAgentRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import com.yego.backend.handler.yego_ticketerera.TicketNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueAgentServiceImpl implements QueueAgentService {

    private final QueueAgentRepository queueAgentRepository;
    private final UserRepository userRepository;
    private final ModuloAtencionRepository moduloAtencionRepository;
    private final ModuloAtencionService moduloAtencionService;
    private final TicketNotificationHandler ticketNotificationHandler;

    @Override
    @Transactional
    public ResponseEntity<Map<String, Object>> liberarModuloDelUsuario(Long userId) {
        Optional<QueueAgent> queueAgent = queueAgentRepository.findByUserIdAndIsActiveTrue(userId);
        if (queueAgent.isPresent()) {
            QueueAgent agent = queueAgent.get();
            Long moduleId = agent.getModuleId();

            agent.setStatus("LIBRE");
            agent.setUpdatedAt(LocalDateTime.now());
            agent.setIsActive(false);

            queueAgentRepository.save(agent);
            moduloAtencionService.cambiarEstadoModulo(moduleId, false);

            ticketNotificationHandler.enviarModulosActualizados(obtenerModulosDisponiblesYOcupados());

            log.info("Módulo {} liberado del usuario {}", moduleId, userId);
            return ResponseEntity.ok(Map.of("message", "Módulo liberado exitosamente", "moduleId", moduleId, "userId", userId));
        }
        return ResponseEntity.ok(Map.of("message", "Usuario no tenía módulo asignado", "userId", userId));
    }

    @Override
    @Transactional
    public ResponseEntity<Map<String, Object>> liberarModuloPorModuleId(Long moduleId) {
        Optional<QueueAgent> queueAgent = queueAgentRepository.findByModuleIdAndIsActiveTrue(moduleId);
        if (queueAgent.isPresent() && "OCUPADO".equals(queueAgent.get().getStatus())) {
            QueueAgent agent = queueAgent.get();
            Long userId = agent.getUserId();

            agent.setStatus("LIBRE");
            agent.setUpdatedAt(LocalDateTime.now());
            agent.setIsActive(false);

            queueAgentRepository.save(agent);
            moduloAtencionService.cambiarEstadoModulo(moduleId, false);

            ticketNotificationHandler.enviarModulosActualizados(obtenerModulosDisponiblesYOcupados());

            log.info("Módulo {} liberado del usuario {}", moduleId, userId);
            return ResponseEntity.ok(Map.of("message", "Módulo liberado exitosamente", "moduleId", moduleId, "userId", userId));
        }
        return ResponseEntity.ok(Map.of("message", "Módulo no está ocupado", "moduleId", moduleId));
    }

    @Override
    @Transactional(readOnly = true)
    public ModulosEstadoResponse obtenerModulosDisponiblesYOcupados() {
        List<ModuloAtencionResponse> modulosDisponibles =
                moduloAtencionService.obtenerTodosLosModulosActivosResponse();

        List<QueueAgent> agentesOcupados = queueAgentRepository.findByStatusAndIsActiveTrue("OCUPADO");

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

        return ModulosEstadoResponse.builder()
                .modulosDisponibles(modulosDisponibles)
                .modulosOcupados(modulosOcupados)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> obtenerQueueAgentIdPorUsuario(Long userId) {
        return queueAgentRepository.findByUserIdAndIsActiveTrue(userId)
                .filter(agent -> "OCUPADO".equals(agent.getStatus()))
                .map(QueueAgent::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecuperarModuloResponse> recuperarModuloAsignado(Long userId) {
        return queueAgentRepository.findByUserIdAndIsActiveTrue(userId)
                .map(queueAgent -> RecuperarModuloResponse.builder()
                        .moduleId(queueAgent.getModuleId())
                        .status(queueAgent.getStatus())
                        .isActive(queueAgent.getIsActive())
                        .createdAt(queueAgent.getCreatedAt())
                        .build());
    }

    @Override
    @Transactional
    public ResponseEntity<AsignarModuloResponse> asignarModuloAUsuario(Map<String, Object> request) {
        try {
            Long userId = ((Number) request.get("userId")).longValue();
            Long moduleId = ((Number) request.get("moduleId")).longValue();
            Long sedeIdRequest = request.get("sedeId") != null
                    ? ((Number) request.get("sedeId")).longValue()
                    : null;

            Long sedeIdUsuario = userRepository.findById(userId).map(User::getSedeId).orElse(null);
            Long sedeId = sedeIdUsuario != null ? sedeIdUsuario : sedeIdRequest;

            if (sedeIdUsuario != null && sedeIdRequest != null && !sedeIdUsuario.equals(sedeIdRequest)) {
                log.warn("Usuario {} pidió módulo de sede {}; se fuerza su sede asignada {}",
                        userId, sedeIdRequest, sedeIdUsuario);
            }

            Optional<QueueAgent> moduloOcupado = queueAgentRepository.findByModuleIdAndIsActiveTrue(moduleId);
            if (moduloOcupado.isPresent() && "OCUPADO".equals(moduloOcupado.get().getStatus())) {
                return ResponseEntity.status(409).build();
            }

            if (sedeId != null) {
                Long sedeModulo = moduloAtencionRepository.findById(moduleId)
                        .map(ModuloAtencion::getSedeId)
                        .orElse(null);
                if (sedeModulo != null && !sedeModulo.equals(sedeId)) {
                    return ResponseEntity.badRequest()
                            .body(AsignarModuloResponse.builder()
                                    .userId(userId)
                                    .moduleId(moduleId)
                                    .status("ERROR: El módulo no pertenece a la sede del usuario")
                                    .build());
                }
            }

            QueueAgent queueAgent = queueAgentRepository.findByUserIdAndIsActiveTrue(userId)
                    .map(existente -> {
                        existente.setModuleId(moduleId);
                        existente.setUpdatedAt(LocalDateTime.now());
                        return existente;
                    })
                    .orElseGet(() -> QueueAgent.builder()
                            .userId(userId)
                            .moduleId(moduleId)
                            .status("OCUPADO")
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .build());

            queueAgentRepository.save(queueAgent);
            moduloAtencionService.cambiarEstadoModulo(moduleId, true);
            ticketNotificationHandler.enviarModulosActualizados(obtenerModulosDisponiblesYOcupados());

            log.info("Módulo {} asignado al usuario {}", moduleId, userId);

            return ResponseEntity.ok(AsignarModuloResponse.builder()
                    .userId(userId)
                    .moduleId(moduleId)
                    .status("OCUPADO")
                    .build());

        } catch (IllegalArgumentException e) {
            log.warn("[QueueAgent] Argumento inválido: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[QueueAgent] Error asignando módulo: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
