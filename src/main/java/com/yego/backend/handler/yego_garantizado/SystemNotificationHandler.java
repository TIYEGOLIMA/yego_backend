package com.yego.backend.handler.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoResponse;
import com.yego.backend.service.yego_principal.FilteredWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SystemNotificationHandler {

    private final FilteredWebSocketService filteredWebSocketService;

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
            filteredWebSocketService.convertAndSend("/topic/system", notification);
            filteredWebSocketService.convertAndSend("/topic/garantizado", notification);
            filteredWebSocketService.convertAndSend("/topic/system-status", notification);
            
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
    
    /**
     * Enviar datos completos de garantizado para actualizar la tabla
     */
    public void enviarDatosCompletosGarantizado(List<GarantizadoResponse> conductores, String semanaActual) {
        log.info("📊 [SystemNotificationHandler] Enviando datos completos de garantizado - {} conductores para semana {}", conductores.size(), semanaActual);

        Map<String, Object> data = Map.of(
            "type", "GARANTIZADO_TABLE_UPDATE",
            "semanaActual", semanaActual,
            "conductores", conductores,
            "totalConductores", conductores.size(),
            "timestamp", LocalDateTime.now().toString()
        );

        // Enviar al topic del sistema
        filteredWebSocketService.convertAndSend("/topic/system", data);

        // Enviar a topic específico de garantizado
        filteredWebSocketService.convertAndSend("/topic/garantizado", data);

        log.info("✅ [SystemNotificationHandler] Datos completos de garantizado enviados - {} conductores", conductores.size());
    }
    
    /**
     * Enviar estado del procesamiento de garantizado (bloqueado/desbloqueado)
     */
    public void enviarEstadoProcesoGarantizado(boolean bloqueado, String mensaje) {
        log.info("🔒 [SystemNotificationHandler] Enviando estado del procesamiento - Bloqueado: {}", bloqueado);
        
        Map<String, Object> evento = Map.of(
            "type", bloqueado ? "GARANTIZADO_BUTTON_BLOCKED" : "GARANTIZADO_BUTTON_UNBLOCKED",
            "bloqueado", bloqueado,
            "mensaje", mensaje,
            "timestamp", LocalDateTime.now().toString()
        );
        
        // Enviar a topic de garantizado
        filteredWebSocketService.convertAndSend("/topic/garantizado", evento);
        
        // También enviar al topic del sistema
        filteredWebSocketService.convertAndSend("/topic/system", evento);
        
        log.info("✅ [SystemNotificationHandler] Estado del procesamiento enviado - Bloqueado: {}", bloqueado);
    }
    
    /**
     * Enviar evento genérico del sistema
     */
    public void sendSystemEvent(String event, Object data) {
        Map<String, Object> notification = Map.of(
            "event", event,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        
        filteredWebSocketService.convertAndSend("/topic/system", notification);
        log.info("📤 [SystemNotificationHandler] Sistema: {}", event);
    }
}

