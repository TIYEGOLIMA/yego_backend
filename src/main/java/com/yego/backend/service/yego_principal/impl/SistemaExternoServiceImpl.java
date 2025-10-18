package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.SistemaExternoRequest;
import com.yego.backend.entity.yego_principal.api.response.SistemaExternoResponse;
import com.yego.backend.entity.yego_principal.entities.SistemaExterno;
import com.yego.backend.event.SistemaExternoEstadoCambiadoEvent;
import com.yego.backend.event.SistemaExternoVerificadoEvent;
import com.yego.backend.repository.yego_principal.SistemaExternoRepository;
import com.yego.backend.service.yego_principal.SistemaExternoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de sistemas externos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SistemaExternoServiceImpl implements SistemaExternoService {
    
    private final SistemaExternoRepository repository;
    private final RestTemplate restTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;
    
    @Override
    public List<SistemaExternoResponse> obtenerTodos() {
        List<SistemaExterno> sistemas = repository.findAll();
        return sistemas.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public SistemaExternoResponse obtenerPorId(Long id) {
        SistemaExterno sistema = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sistema externo no encontrado"));
        return mapToResponse(sistema);
    }
    
    @Override
    @Transactional
    public SistemaExternoResponse crear(SistemaExternoRequest request) {
        // Validar que la URL esté presente
        if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL es obligatoria");
        }
        
        // Verificar que la URL no exista
        if (repository.findByYegoSisExtUrl(request.getUrl()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un sistema con esa URL");
        }
        
        SistemaExterno sistema = SistemaExterno.builder()
                .yegoSisExtNombre(request.getNombre())
                .yegoSisExtDescripcion(request.getDescripcion())
                .yegoSisExtUrl(request.getUrl())
                .yegoSisExtEstado(request.getEstado() != null ? request.getEstado() : SistemaExterno.EstadoSistema.INACTIVO)
                .yegoSisExtActivo(request.getActivo() != null ? request.getActivo() : true)
                .build();
        
        SistemaExterno saved = repository.save(sistema);
        log.info("✅ Sistema externo creado: {} - {}", saved.getYegoSisExtId(), saved.getYegoSisExtNombre());
        
        return mapToResponse(saved);
    }
    
    @Override
    @Transactional
    public SistemaExternoResponse actualizar(Long id, SistemaExternoRequest request) {
        SistemaExterno sistema = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sistema externo no encontrado"));

        // Validar que la URL esté presente
        if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL es obligatoria");
        }

        // Verificar que la URL no exista en otro sistema
        repository.findByYegoSisExtUrl(request.getUrl())
                .ifPresent(existing -> {
                    if (!existing.getYegoSisExtId().equals(id)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un sistema con esa URL");
                    }
                });
        
        sistema.setYegoSisExtNombre(request.getNombre());
        sistema.setYegoSisExtDescripcion(request.getDescripcion());
        sistema.setYegoSisExtUrl(request.getUrl());
        sistema.setYegoSisExtEstado(request.getEstado() != null ? request.getEstado() : sistema.getYegoSisExtEstado());
        sistema.setYegoSisExtActivo(request.getActivo() != null ? request.getActivo() : sistema.getYegoSisExtActivo());
        
        SistemaExterno updated = repository.save(sistema);
        log.info("✅ Sistema externo actualizado: {} - {}", updated.getYegoSisExtId(), updated.getYegoSisExtNombre());
        
        return mapToResponse(updated);
    }
    
    @Override
    @Transactional
    public SistemaExternoResponse cambiarEstado(Long id, SistemaExterno.EstadoSistema nuevoEstado) {
        SistemaExterno sistema = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sistema externo no encontrado"));
        
        SistemaExterno.EstadoSistema estadoAnterior = sistema.getYegoSisExtEstado();
        sistema.setYegoSisExtEstado(nuevoEstado);
        sistema.setYegoSisExtUltimoCheck(LocalDateTime.now());
        
        SistemaExterno updated = repository.save(sistema);
        log.info("✅ Estado cambiado para sistema {}: {} -> {}", 
                updated.getYegoSisExtNombre(), estadoAnterior, nuevoEstado);
        
        // Enviar evento WebSocket
        SistemaExternoEstadoCambiadoEvent event = SistemaExternoEstadoCambiadoEvent.fromSistema(
                updated, estadoAnterior);
        
        messagingTemplate.convertAndSend("/topic/sistemas-externos", event);
        messagingTemplate.convertAndSend("/topic/sistema-estado-cambiado", event);
        log.info("📡 Evento WebSocket enviado: Sistema {} cambió estado", updated.getYegoSisExtNombre());
        
        return mapToResponse(updated);
    }
    
    @Override
    @Transactional
    public SistemaExternoResponse verificarEstado(Long id) {
        SistemaExterno sistema = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sistema externo no encontrado"));
        
        Boolean exitoso = false;
        String mensaje = "";
        
        try {
            log.info("🔍 Verificando estado de sistema: {} - {}", sistema.getYegoSisExtNombre(), sistema.getYegoSisExtUrl());
            
            // Hacer una petición HEAD para verificar si responde
            restTemplate.headForHeaders(sistema.getYegoSisExtUrl());
            
            sistema.setYegoSisExtEstado(SistemaExterno.EstadoSistema.ACTIVO);
            exitoso = true;
            mensaje = "Sistema responde correctamente";
            log.info("✅ Sistema {} está ACTIVO", sistema.getYegoSisExtNombre());
            
        } catch (Exception e) {
            sistema.setYegoSisExtEstado(SistemaExterno.EstadoSistema.INACTIVO);
            exitoso = false;
            mensaje = "Sistema no responde: " + e.getMessage();
            log.warn("⚠️ Sistema {} está INACTIVO (no responde): {}", sistema.getYegoSisExtNombre(), e.getMessage());
        }
        
        sistema.setYegoSisExtUltimoCheck(LocalDateTime.now());
        SistemaExterno updated = repository.save(sistema);
        
        // Enviar evento WebSocket
        SistemaExternoVerificadoEvent event = SistemaExternoVerificadoEvent.fromSistema(
                updated, exitoso, mensaje);
        
        messagingTemplate.convertAndSend("/topic/sistemas-externos", event);
        messagingTemplate.convertAndSend("/topic/sistema-verificado", event);
        log.info("📡 Evento WebSocket enviado: Sistema {} verificado", updated.getYegoSisExtNombre());
        
        return mapToResponse(updated);
    }
    
    @Override
    @Transactional
    public void eliminar(Long id) {
        SistemaExterno sistema = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sistema externo no encontrado"));
        
        repository.delete(sistema);
        log.info("✅ Sistema externo eliminado: {} - {}", id, sistema.getYegoSisExtNombre());
    }
    
    @Override
    public List<SistemaExternoResponse> buscarPorTermino(String termino) {
        List<SistemaExterno> sistemas = repository.buscarPorTermino(termino);
        return sistemas.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SistemaExternoResponse toggleActivo(Long id, Boolean activo) {
        SistemaExterno sistema = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sistema externo no encontrado"));

        sistema.setYegoSisExtActivo(activo);
        sistema.setYegoSisExtUltimoCheck(LocalDateTime.now());
        
        // 🚨 CAMBIAR TAMBIÉN EL ESTADO DEL SISTEMA
        if (!activo) {
            // Si se desactiva, cambiar estado a INACTIVO
            sistema.setYegoSisExtEstado(SistemaExterno.EstadoSistema.INACTIVO);
            log.info("🔄 Estado del sistema cambiado a INACTIVO por desactivación");
        } else {
            // Si se activa, cambiar estado a ACTIVO
            sistema.setYegoSisExtEstado(SistemaExterno.EstadoSistema.ACTIVO);
            log.info("🔄 Estado del sistema cambiado a ACTIVO por activación");
        }

        SistemaExterno updated = repository.save(sistema);
        log.info("✅ Estado activo cambiado para sistema {}: {} (Estado: {})", 
                updated.getYegoSisExtNombre(), activo, updated.getYegoSisExtEstado());

        // Enviar notificación de cambio de estado (activado o desactivado)
        enviarNotificacionCambioEstado(updated, activo);

        return mapToResponse(updated);
    }

    /**
     * Envía notificación de cambio de estado (activado/desactivado) al sistema externo
     */
    private void enviarNotificacionCambioEstado(SistemaExterno sistema, Boolean activo) {
        try {
            String accion = activo ? "ACTIVO" : "INACTIVO";
            log.info("📡 Sistema {} {}, enviando notificaciones WebSocket...", sistema.getYegoSisExtNombre(), accion);
            
            // Construir el mensaje según el estado
            Map<String, Object> webSocketMessage = new HashMap<>();
            
            if (activo) {
                webSocketMessage.put("type", "SYSTEM_ACTIVATED");
                webSocketMessage.put("message", "El sistema ha sido activado. Puedes procesar solicitudes normalmente.");
                webSocketMessage.put("action", "ENABLE_SCREEN");
            } else {
                webSocketMessage.put("type", "SYSTEM_DEACTIVATED");
                webSocketMessage.put("message", "Este sistema ha sido desactivado. Deja de procesar solicitudes.");
                webSocketMessage.put("action", "BLOCK_SCREEN");
            }
            
            webSocketMessage.put("timestamp", LocalDateTime.now().toString());
            webSocketMessage.put("sistemaId", sistema.getYegoSisExtId());
            webSocketMessage.put("sistemaNombre", sistema.getYegoSisExtNombre());
            webSocketMessage.put("sistemaUrl", sistema.getYegoSisExtUrl());
            webSocketMessage.put("activo", activo);

            // Enviar WebSocket al Frontend
            messagingTemplate.convertAndSend("/topic/sistemas-externos", webSocketMessage);
            messagingTemplate.convertAndSend("/topic/system", webSocketMessage);
            
            // ✅ Solo WebSocket - El Frontend se conecta y recibe la notificación
            
            log.info("✅ Notificaciones WebSocket de {} enviadas para sistema: {}", accion, sistema.getYegoSisExtNombre());
            
        } catch (Exception e) {
            log.error("❌ Error enviando notificación WebSocket de cambio de estado a sistema {}: {}", 
                    sistema.getYegoSisExtNombre(), e.getMessage());
        }
    }
    

    
    private SistemaExternoResponse mapToResponse(SistemaExterno sistema) {
        return SistemaExternoResponse.builder()
                .id(sistema.getYegoSisExtId())
                .nombre(sistema.getYegoSisExtNombre())
                .descripcion(sistema.getYegoSisExtDescripcion())
                .url(sistema.getYegoSisExtUrl())
                .estado(sistema.getYegoSisExtEstado())
                .ultimoCheck(sistema.getYegoSisExtUltimoCheck())
                .activo(sistema.getYegoSisExtActivo())
                .createdAt(sistema.getYegoSisExtCreatedAt())
                .updatedAt(sistema.getYegoSisExtUpdatedAt())
                .build();
    }
}
