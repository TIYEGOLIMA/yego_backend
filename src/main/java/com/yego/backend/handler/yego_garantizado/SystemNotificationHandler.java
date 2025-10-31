package com.yego.backend.handler.yego_garantizado;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


@Component
@Slf4j
@RequiredArgsConstructor
public class SystemNotificationHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Envía notificación de estado del sistema a todos los clientes conectados
     * @param isActive true si el sistema está activo, false si está desactivado
     * @param message mensaje descriptivo del cambio
     */
    public void broadcastSystemStatus(boolean isActive, String message) {
        try {
            Map<String, Object> notification = Map.of(
                "type", isActive ? "SYSTEM_ACTIVATED" : "SYSTEM_DEACTIVATED",
                "systemActive", isActive,
                "message", message,
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                "nextActivation", calculateNextActivation()
            );
            
            // Enviar a todos los tópicos del sistema
            messagingTemplate.convertAndSend("/topic/system", notification);
            messagingTemplate.convertAndSend("/topic/garantizado", notification);
            messagingTemplate.convertAndSend("/topic/system-status", notification);
            
            log.info("📢 Notificación WebSocket enviada: {}", notification);
        } catch (Exception e) {
            log.error("❌ Error enviando notificación WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Calcula la próxima activación del sistema
     */
    private String calculateNextActivation() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        
        if (currentDay == DayOfWeek.SATURDAY) {
            // Si es sábado, próxima activación es lunes 6:00 AM
            return now.plusDays(2).with(LocalTime.of(6, 0))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } else if (currentDay == DayOfWeek.SUNDAY) {
            // Si es domingo, próxima activación es lunes 6:00 AM
            return now.plusDays(1).with(LocalTime.of(6, 0))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } else if (currentDay == DayOfWeek.FRIDAY && now.toLocalTime().isAfter(LocalTime.of(23, 59))) {
            // Si es viernes después de 23:59, próxima activación es lunes 6:00 AM
            return now.plusDays(3).with(LocalTime.of(6, 0))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } else {
            // Si es lunes antes de 6:00 AM, próxima activación es hoy 6:00 AM
            return now.with(LocalTime.of(6, 0))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }
}

