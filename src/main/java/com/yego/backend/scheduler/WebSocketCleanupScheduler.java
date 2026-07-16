package com.yego.backend.scheduler;

import com.yego.backend.service.yego_principal.WebSocketSessionService;
import com.yego.backend.service.yego_principal.WebSocketConnectionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

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
    private final WebSocketConnectionRegistry connectionRegistry;

    @Scheduled(fixedRate = 60000)
    public void closeExpiredTokenConnections() {
        for (String sessionId : webSocketSessionService.getExpiredSessionIds()) {
            connectionRegistry.close(sessionId, new CloseStatus(4001, "JWT expired"));
            webSocketSessionService.removeSession(sessionId);
        }
    }
    
    /**
     * Limpia conexiones WebSocket inactivas cada 30 minutos
     * Ejecuta: cada 30 minutos (1800000 ms)
     * Limpia conexiones sin actividad por más de 5 horas
     * 
     * Nota: Ejecutar cada 30 minutos permite limpiar conexiones colgadas más rápidamente
     * y evitar que se alcance el límite de 500 conexiones.
     */
    @Scheduled(fixedRate = 1800000) // 30 minutos
    public void cleanupInactiveWebSocketConnections() {
        try {
            int cleaned = webSocketSessionService.cleanupInactiveSessions();
            
            // Log de estadísticas en cada ejecución
            var stats = webSocketSessionService.getConnectionStats();
            int total = (Integer) stats.get("total");
            int inactive = (Integer) stats.get("inactive");
            int maxConnections = (Integer) stats.get("maxConnections");
            
            if (cleaned > 0) {
                log.info("🧹 [WebSocketCleanup] Limpiadas {} conexiones inactivas (> 5 horas sin actividad)", cleaned);
            }
            
            // Advertencia si estamos cerca del límite
            if (total >= maxConnections * 0.9) {
                log.warn("⚠️ [WebSocketCleanup] Cerca del límite: {}/{} conexiones ({} inactivas)", 
                    total, maxConnections, inactive);
            } else {
                log.info("📊 [WebSocketCleanup] Estadísticas: Total={}, Activas={}, Inactivas={}, Máximo={}", 
                    total, stats.get("active"), inactive, maxConnections);
            }
        } catch (Exception e) {
            log.error("❌ [WebSocketCleanup] Error limpiando conexiones WebSocket: {}", e.getMessage(), e);
        }
    }
}
