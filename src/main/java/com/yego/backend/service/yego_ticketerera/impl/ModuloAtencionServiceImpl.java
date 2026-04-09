package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloUsuarioResponse;
import com.yego.backend.handler.yego_ticketerera.TicketNotificationHandler;
import com.yego.backend.repository.yego_ticketerera.ModuloAtencionRepository;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import com.yego.backend.service.yego_ticketerera.QueueAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de Módulos de Atención del sistema YEGO Ticketerera
 */
@Service
@Slf4j
public class ModuloAtencionServiceImpl implements ModuloAtencionService {
    
    private final ModuloAtencionRepository moduloAtencionRepository;
    private final QueueAgentService queueAgentService;
    private final TicketNotificationHandler ticketNotificationHandler;
    
    public ModuloAtencionServiceImpl(
            ModuloAtencionRepository moduloAtencionRepository,
            @Lazy QueueAgentService queueAgentService,
            TicketNotificationHandler ticketNotificationHandler) {
        this.moduloAtencionRepository = moduloAtencionRepository;
        this.queueAgentService = queueAgentService;
        this.ticketNotificationHandler = ticketNotificationHandler;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ModuloAtencionResponse> obtenerTodosLosModulosActivosResponse() {
        log.debug("Módulos de atención disponibles (response)");
        List<ModuloAtencion> modules = moduloAtencionRepository.findByIsActiveFalseOrderByNameAsc();
        List<ModuloAtencionResponse> modulosResponse = modules.stream()
                .map(ModuloAtencionResponse::fromModuloAtencion)
                .toList();
        log.debug("Módulos disponibles: {}", modulosResponse.size());
        return modulosResponse;
    }
    
    @Override
    @Transactional
    public void cambiarEstadoModulo(Long moduleId, boolean activo) {
        
        Optional<ModuloAtencion> moduloOpt = moduloAtencionRepository.findById(moduleId);
        if (moduloOpt.isPresent()) {
            ModuloAtencion modulo = moduloOpt.get();
            modulo.setIsActive(activo);
            moduloAtencionRepository.save(modulo);
        } else {
            log.warn("Módulo {} no encontrado", moduleId);
            throw new IllegalArgumentException("Módulo no encontrado con ID: " + moduleId);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ModuloUsuarioResponse verificarModuloOListarDisponibles(Long userId) {
        log.debug("Módulo para usuario {} o lista disponibles", userId);
        
        // Verificar si el usuario tiene módulo asignado
        var moduloAsignadoOpt = queueAgentService.recuperarModuloAsignado(userId);
        
        ModuloUsuarioResponse response;
        if (moduloAsignadoOpt.isPresent()) {
            log.debug("Usuario {} módulo {} asignado", userId, moduloAsignadoOpt.get().getModuleId());
            response = ModuloUsuarioResponse.builder()
                    .tieneModuloAsignado(true)
                    .moduloAsignado(moduloAsignadoOpt.get())
                    .modulosDisponibles(null)
                    .build();
        } else {
            log.debug("Usuario {} sin módulo; listando ocupados y disponibles", userId);
            
            // Obtener módulos disponibles (isActive = false)
            List<ModuloAtencion> modulesDisponibles = moduloAtencionRepository.findByIsActiveFalseOrderByNameAsc();
            List<ModuloAtencionResponse> modulosDisponiblesResponse = modulesDisponibles.stream()
                    .map(ModuloAtencionResponse::fromModuloAtencion)
                    .toList();
            
            // Obtener módulos ocupados con información del usuario usando el método del servicio
            var modulosEstado = queueAgentService.obtenerModulosDisponiblesYOcupados();
            
            response = ModuloUsuarioResponse.builder()
                    .tieneModuloAsignado(false)
                    .moduloAsignado(null)
                    .modulosDisponibles(modulosDisponiblesResponse)
                    .modulosOcupados(modulosEstado.getModulosOcupados())
                    .build();
        }
        
        // Enviar lista actualizada de módulos por WebSocket para que perdure la información
        try {
            var modulosEstado = queueAgentService.obtenerModulosDisponiblesYOcupados();
            ticketNotificationHandler.enviarModulosActualizados(modulosEstado);
            log.debug("WebSocket módulos actualizados para usuario {}", userId);
        } catch (Exception e) {
            log.error("Error enviando WebSocket módulos: {}", e.getMessage(), e);
        }
        
        return response;
    }
}
