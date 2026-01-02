package com.yego.backend.handler.yego_asistencia;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Handler para notificaciones WebSocket relacionadas con asistencia
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceNotificationHandler {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Enviar actualización de registros de hoy
     */
    public void enviarActualizacionRegistrosHoy(Long userId, List<Map<String, Object>> registrosHoy) {
        log.info("📤 [AttendanceNotificationHandler] Enviando actualización de registros de hoy - Usuario: {}, Registros: {}", userId, registrosHoy.size());
        
        Map<String, Object> evento = Map.of(
            "type", "TODAY_RECORDS_UPDATE",
            "userId", userId,
            "registrosHoy", registrosHoy,
            "total", registrosHoy.size(),
            "timestamp", LocalDateTime.now().toString()
        );
        
        // Enviar al usuario específico
        messagingTemplate.convertAndSend("/topic/user/" + userId, evento);
        log.info("✅ [AttendanceNotificationHandler] Actualización de registros de hoy enviada - Usuario: {}, Total: {}", userId, registrosHoy.size());
    }
}

