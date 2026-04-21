package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearModuloAtencionRequest;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        return mapearConSedesEnLote(moduloAtencionRepository.findByIsActiveTrueOrderByNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuloAtencionResponse> listarTodos(Long sedeId) {
        List<ModuloAtencion> modulos = (sedeId != null)
                ? moduloAtencionRepository.findBySedeIdOrderByNameAsc(sedeId)
                : moduloAtencionRepository.findAllByOrderByNameAsc();
        return mapearConSedesEnLote(modulos);
    }

    @Override
    @Transactional
    public ModuloAtencionResponse crear(CrearModuloAtencionRequest request) {
        validarSedeExiste(request.getSedeId());
        if (moduloAtencionRepository.existsByNameIgnoreCaseAndSedeId(request.getName().trim(), request.getSedeId())) {
            throw new IllegalArgumentException("Ya existe un módulo con ese nombre en la sede seleccionada");
        }

        ModuloAtencion modulo = ModuloAtencion.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .sedeId(request.getSedeId())
                .isActive(true)
                .build();

        ModuloAtencion guardado = moduloAtencionRepository.save(modulo);
        return ModuloAtencionResponse.fromModuloAtencion(guardado, resolverSedeNombre(guardado.getSedeId()));
    }

    @Override
    @Transactional
    public ModuloAtencionResponse actualizar(Long id, CrearModuloAtencionRequest request) {
        ModuloAtencion modulo = moduloAtencionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Módulo no encontrado con ID: " + id));
        validarSedeExiste(request.getSedeId());

        String nuevoNombre = request.getName().trim();
        if (moduloAtencionRepository.existsByNameIgnoreCaseAndSedeIdAndIdNot(nuevoNombre, request.getSedeId(), id)) {
            throw new IllegalArgumentException("Ya existe otro módulo con ese nombre en la sede seleccionada");
        }

        modulo.setName(nuevoNombre);
        modulo.setDescription(request.getDescription());
        modulo.setSedeId(request.getSedeId());

        ModuloAtencion guardado = moduloAtencionRepository.save(modulo);
        return ModuloAtencionResponse.fromModuloAtencion(guardado, resolverSedeNombre(guardado.getSedeId()));
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
    @Transactional
    public void eliminar(Long id) {
        if (!moduloAtencionRepository.existsById(id)) {
            throw new IllegalArgumentException("Módulo no encontrado con ID: " + id);
        }
        try {
            moduloAtencionRepository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "No se puede eliminar el módulo porque tiene tickets u otros registros asociados. Desactívalo en su lugar.");
        }
    }

    private void validarSedeExiste(Long sedeId) {
        if (sedeId == null || !sedeRepository.existsById(sedeId)) {
            throw new IllegalArgumentException("Sede no encontrada con ID: " + sedeId);
        }
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

        var estadoModulos = queueAgentService.obtenerModulosDisponiblesYOcupados();

        ModuloUsuarioResponse response;
        if (moduloAsignadoOpt.isPresent()) {
            response = ModuloUsuarioResponse.builder()
                    .tieneModuloAsignado(true)
                    .moduloAsignado(moduloAsignadoOpt.get())
                    .build();
        } else {
            List<ModuloAtencion> disponibles = (sedeId != null)
                    ? moduloAtencionRepository.findBySedeIdAndIsActiveTrueOrderByNameAsc(sedeId)
                    : moduloAtencionRepository.findByIsActiveTrueOrderByNameAsc();
            String sedeNombre = resolverSedeNombre(sedeId);

            response = ModuloUsuarioResponse.builder()
                    .tieneModuloAsignado(false)
                    .modulosDisponibles(disponibles.stream()
                            .map(m -> ModuloAtencionResponse.fromModuloAtencion(m, sedeNombre))
                            .toList())
                    .modulosOcupados(estadoModulos.getModulosOcupados())
                    .build();
        }

        try {
            ticketNotificationHandler.enviarModulosActualizados(estadoModulos);
        } catch (Exception e) {
            log.error("Error enviando WebSocket módulos: {}", e.getMessage(), e);
        }

        return response;
    }

    private String resolverSedeNombre(Long sedeId) {
        return sedeId != null ? sedeRepository.findById(sedeId).map(Sede::getName).orElse(null) : null;
    }

    private List<ModuloAtencionResponse> mapearConSedesEnLote(List<ModuloAtencion> modulos) {
        if (modulos.isEmpty()) return List.of();
        List<Long> sedeIds = modulos.stream()
                .map(ModuloAtencion::getSedeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> sedesMap = sedeIds.isEmpty()
                ? Map.of()
                : sedeRepository.findAllById(sedeIds).stream()
                        .collect(Collectors.toMap(Sede::getId, Sede::getName));
        return modulos.stream()
                .map(m -> ModuloAtencionResponse.fromModuloAtencion(m, sedesMap.get(m.getSedeId())))
                .toList();
    }
}
