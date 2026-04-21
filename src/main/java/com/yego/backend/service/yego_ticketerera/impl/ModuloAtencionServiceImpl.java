package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloUsuarioResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Sede;
import com.yego.backend.handler.yego_ticketerera.TicketNotificationHandler;
import com.yego.backend.repository.yego_ticketerera.ModuloAtencionRepository;
import com.yego.backend.repository.yego_ticketerera.SedeRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ModuloAtencionServiceImpl implements ModuloAtencionService {
    
    private final ModuloAtencionRepository moduloAtencionRepository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;
    private final QueueAgentService queueAgentService;
    private final TicketNotificationHandler ticketNotificationHandler;

    public ModuloAtencionServiceImpl(
            ModuloAtencionRepository moduloAtencionRepository,
            SedeRepository sedeRepository,
            UserRepository userRepository,
            @Lazy QueueAgentService queueAgentService,
            TicketNotificationHandler ticketNotificationHandler) {
        this.moduloAtencionRepository = moduloAtencionRepository;
        this.sedeRepository = sedeRepository;
        this.userRepository = userRepository;
        this.queueAgentService = queueAgentService;
        this.ticketNotificationHandler = ticketNotificationHandler;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ModuloAtencionResponse> obtenerTodosLosModulosActivosResponse() {
        return moduloAtencionRepository.findByIsActiveFalseOrderByNameAsc().stream()
                .map(m -> ModuloAtencionResponse.fromModuloAtencion(m, resolverSedeNombre(m.getSedeId())))
                .toList();
    }

    @Override
    @Transactional
    public void cambiarEstadoModulo(Long moduleId, boolean activo) {
        ModuloAtencion modulo = moduloAtencionRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Módulo no encontrado con ID: " + moduleId));
        modulo.setIsActive(activo);
        moduloAtencionRepository.save(modulo);
    }

    @Override
    @Transactional(readOnly = true)
    public ModuloUsuarioResponse verificarModuloOListarDisponibles(Long userId, Long sedeIdRequest) {
        Long sedeIdUsuario = userRepository.findById(userId).map(User::getSedeId).orElse(null);
        Long sedeId = sedeIdUsuario != null ? sedeIdUsuario : sedeIdRequest;

        if (sedeIdUsuario != null && sedeIdRequest != null && !sedeIdUsuario.equals(sedeIdRequest)) {
            log.warn("Usuario {} pidió módulos de sede {}; se fuerza su sede asignada {}",
                    userId, sedeIdRequest, sedeIdUsuario);
        }

        var moduloAsignadoOpt = queueAgentService.recuperarModuloAsignado(userId);

        ModuloUsuarioResponse response;
        if (moduloAsignadoOpt.isPresent()) {
            response = ModuloUsuarioResponse.builder()
                    .tieneModuloAsignado(true)
                    .moduloAsignado(moduloAsignadoOpt.get())
                    .build();
        } else {
            List<ModuloAtencion> disponibles = (sedeId != null)
                    ? moduloAtencionRepository.findBySedeIdAndIsActiveFalseOrderByNameAsc(sedeId)
                    : moduloAtencionRepository.findByIsActiveFalseOrderByNameAsc();
            String sedeNombre = resolverSedeNombre(sedeId);

            response = ModuloUsuarioResponse.builder()
                    .tieneModuloAsignado(false)
                    .modulosDisponibles(disponibles.stream()
                            .map(m -> ModuloAtencionResponse.fromModuloAtencion(m, sedeNombre))
                            .toList())
                    .modulosOcupados(queueAgentService.obtenerModulosDisponiblesYOcupados().getModulosOcupados())
                    .build();
        }

        try {
            ticketNotificationHandler.enviarModulosActualizados(queueAgentService.obtenerModulosDisponiblesYOcupados());
        } catch (Exception e) {
            log.error("Error enviando WebSocket módulos: {}", e.getMessage(), e);
        }

        return response;
    }

    private String resolverSedeNombre(Long sedeId) {
        return sedeId != null ? sedeRepository.findById(sedeId).map(Sede::getName).orElse(null) : null;
    }
}
