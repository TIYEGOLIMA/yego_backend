package com.yego.backend.scheduler;

import com.yego.backend.service.yego_principal.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler para limpiar conexiones WebSocket inactivas
 * Evita la acumulación de conexiones colgadas que causan "Too many open files"
 * Limpia conexiones sin actividad por más de 5 horas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketCleanupScheduler {
    
    private final WebSocketSessionService webSocketSessionService;
    
    /**
     * Limpia conexiones WebSocket inactivas cada 4 horas
     * Ejecuta: cada 4 horas (14400000 ms)
     * Limpia conexiones sin actividad por más de 5 horas
     * 
     * Nota: Con un timeout de 5 horas, verificar cada 4 horas es suficiente
     * para mantener el sistema limpio sin sobrecargar.
     */
    @Scheduled(fixedRate = 14400000) // 4 horas
    public void cleanupInactiveWebSocketConnections() {
        try {
            int cleaned = webSocketSessionService.cleanupInactiveSessions();
            
            if (cleaned > 0) {
                log.info("🧹 [WebSocketCleanup] Limpiadas {} conexiones inactivas (> 5 horas sin actividad)", cleaned);
            }
            
            // Log de estadísticas en cada ejecución (cada 4 horas)
            var stats = webSocketSessionService.getConnectionStats();
            log.info("📊 [WebSocketCleanup] Estadísticas: Total={}, Activas={}, Inactivas={}, Máximo={}", 
                stats.get("total"), stats.get("active"), stats.get("inactive"), stats.get("maxConnections"));
        } catch (Exception e) {
            log.error("❌ [WebSocketCleanup] Error limpiando conexiones WebSocket: {}", e.getMessage(), e);
        }
    }
}

