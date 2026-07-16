package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar sesiones WebSocket
 * Maneja conexiones, suscripciones y limpieza automática de sesiones inactivas
 */
@Slf4j
@Service
public class WebSocketSessionService {
    
    // Límite máximo de conexiones WebSocket simultáneas
    private static final int MAX_CONNECTIONS = 500;
    
    // Timeout de inactividad: 5 horas sin actividad = conexión muerta (limpieza normal)
    private static final int INACTIVITY_TIMEOUT_MINUTES = 300; // 5 horas
    
    // Timeout agresivo cuando se alcanza el límite: 30 minutos sin actividad
    private static final int AGGRESSIVE_CLEANUP_TIMEOUT_MINUTES = 30; // 30 minutos
    
    // Almacenamiento de sesiones WebSocket
    private final Map<String, List<ModuleResponse>> sessionModules = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserIds = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> sessionLastActivity = new ConcurrentHashMap<>();
    private final Set<String> deviceSessions = ConcurrentHashMap.newKeySet();
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    public void markAsDevice(String sessionId, String deviceId) {
        if (sessionId == null) return;
        deviceSessions.add(sessionId);
        if (deviceId != null) {
            sessionUserIds.put(sessionId, deviceId);
        }
        updateLastActivity(sessionId);
    }

    public boolean isDeviceSession(String sessionId) {
        return sessionId != null && deviceSessions.contains(sessionId);
    }

    public void saveSessionContext(String sessionId, SessionContext context) {
        if (sessionId == null || context == null) return;
        sessionContexts.put(sessionId, context);
        if (context.device()) deviceSessions.add(sessionId);
        updateLastActivity(sessionId);
    }

    public SessionContext getSessionContext(String sessionId) {
        return sessionId != null ? sessionContexts.get(sessionId) : null;
    }

    public boolean isSessionExpired(String sessionId) {
        SessionContext context = getSessionContext(sessionId);
        return context != null && context.tokenExpiresAt() != null
                && !context.tokenExpiresAt().isAfter(Instant.now());
    }

    public Set<String> getExpiredSessionIds() {
        Instant now = Instant.now();
        return sessionContexts.entrySet().stream()
                .filter(entry -> entry.getValue().tokenExpiresAt() != null
                        && !entry.getValue().tokenExpiresAt().isAfter(now))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    
    public void saveUserModules(String sessionId, List<ModuleResponse> modules, String userId) {
        if (sessionId != null && modules != null) {
            // Verificar límite de conexiones
            int currentConnections = sessionModules.size();
            if (currentConnections >= MAX_CONNECTIONS) {
                log.warn("⚠️ [WebSocket] Límite de conexiones alcanzado: {}/{} - Limpiando conexiones inactivas...", currentConnections, MAX_CONNECTIONS);
                
                // Limpieza agresiva: conexiones inactivas por más de 30 minutos + conexiones antiguas
                int cleaned = cleanupInactiveSessions(AGGRESSIVE_CLEANUP_TIMEOUT_MINUTES);
                log.info("🧹 [WebSocket] Limpieza agresiva: {} conexiones removidas (> 30 min o sin lastActivity)", cleaned);
                
                // Si aún está lleno, intentar limpiar conexiones más recientes (10 minutos)
                if (sessionModules.size() >= MAX_CONNECTIONS) {
                    log.warn("⚠️ [WebSocket] Aún en límite después de limpieza agresiva, intentando limpieza más agresiva (10 min)...");
                    int cleaned2 = cleanupInactiveSessions(10); // 10 minutos
                    log.info("🧹 [WebSocket] Segunda limpieza: {} conexiones removidas (> 10 min)", cleaned2);
                }
                
                // Si aún está lleno después de ambas limpiezas, rechazar nueva conexión
                if (sessionModules.size() >= MAX_CONNECTIONS) {
                    log.error("❌ [WebSocket] Rechazando nueva conexión: límite alcanzado después de limpiezas (Total: {})", sessionModules.size());
                    throw new IllegalStateException("Límite de conexiones WebSocket alcanzado");
                }
            }
            
            sessionModules.put(sessionId, modules);
            if (userId != null) {
                sessionUserIds.put(sessionId, userId);
            }
            updateLastActivity(sessionId);
            log.debug("✅ [WebSocket] Sesión {} registrada (Total: {})", sessionId, sessionModules.size());
        }
    }
    
    /**
     * Actualiza la última actividad de una sesión
     */
    public void updateLastActivity(String sessionId) {
        if (sessionId != null) {
            sessionLastActivity.put(sessionId, LocalDateTime.now());
        }
    }
    
    public List<ModuleResponse> getUserModules(String sessionId) {
        return sessionId != null ? sessionModules.get(sessionId) : null;
    }
    
    public String getUserId(String sessionId) {
        return sessionId != null ? sessionUserIds.get(sessionId) : null;
    }
    
    public void addSubscription(String sessionId, String topic) {
        if (sessionId != null && topic != null) {
            sessionSubscriptions.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(topic);
            updateLastActivity(sessionId);
        }
    }
    
    public Set<String> getSessionsSubscribedTo(String topic) {
        return sessionSubscriptions.entrySet().stream()
            .filter(entry -> entry.getValue().contains(topic))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }
    
    /**
     * Obtiene todas las sesiones que tienen acceso a un módulo específico
     * 
     * @param modulePattern Patrón del módulo (ej: "tickets", "pro-ops")
     * @return Set de sessionIds con acceso al módulo
     */
    public Set<String> getSessionsWithModuleAccess(String modulePattern) {
        if (modulePattern == null) {
            return Set.of();
        }
        
        String finalPattern = modulePattern.toLowerCase();
        
        return sessionModules.entrySet().stream()
            .filter(entry -> {
                List<ModuleResponse> modules = entry.getValue();
                if (modules == null || modules.isEmpty()) {
                    return false;
                }
                
                return modules.stream()
                    .anyMatch(module -> {
                        String url = module.getUrl() != null ? module.getUrl().toLowerCase() : "";
                        String nombre = module.getNombre() != null ? module.getNombre().toLowerCase() : "";
                        return url.contains(finalPattern) || nombre.contains(finalPattern);
                    });
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }
    
    public void removeSession(String sessionId) {
        if (sessionId != null) {
            sessionModules.remove(sessionId);
            sessionUserIds.remove(sessionId);
            sessionSubscriptions.remove(sessionId);
            sessionLastActivity.remove(sessionId);
            deviceSessions.remove(sessionId);
            sessionContexts.remove(sessionId);
            log.debug("🔌 [WebSocket] Sesión {} removida (Total: {})", sessionId, sessionModules.size());
        }
    }
    
    /**
     * Limpia sesiones inactivas (sin actividad por más de 5 horas)
     * @return Número de sesiones limpiadas
     */
    public int cleanupInactiveSessions() {
        return cleanupInactiveSessions(INACTIVITY_TIMEOUT_MINUTES);
    }
    
    /**
     * Limpia sesiones inactivas con timeout personalizado
     * También limpia conexiones antiguas sin lastActivity registrado
     * @param timeoutMinutes Timeout en minutos
     * @return Número de sesiones limpiadas
     */
    public int cleanupInactiveSessions(int timeoutMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        int cleaned = 0;
        int cleanedOld = 0;
        
        // Primero: limpiar conexiones sin lastActivity (conexiones antiguas)
        // Crear copia de keys para evitar ConcurrentModificationException
        Set<String> sessionIdsToCheck = new HashSet<>(sessionModules.keySet());
        for (String sessionId : sessionIdsToCheck) {
            if (!sessionLastActivity.containsKey(sessionId)) {
                removeSession(sessionId);
                cleanedOld++;
            }
        }
        
        // Segundo: limpiar conexiones inactivas por más del timeout
        // Crear copia de entries para evitar ConcurrentModificationException
        Set<Map.Entry<String, LocalDateTime>> entriesToCheck = new HashSet<>(sessionLastActivity.entrySet());
        for (Map.Entry<String, LocalDateTime> entry : entriesToCheck) {
            String sessionId = entry.getKey();
            LocalDateTime lastActivity = entry.getValue();
            
            if (lastActivity == null || lastActivity.isBefore(cutoff)) {
                removeSession(sessionId);
                cleaned++;
            }
        }
        
        int totalCleaned = cleaned + cleanedOld;
        if (totalCleaned > 0) {
            log.info("🧹 [WebSocket] Limpiadas {} sesiones: {} inactivas (> {} min) + {} antiguas (sin lastActivity) (Total activas: {})", 
                totalCleaned, cleaned, timeoutMinutes, cleanedOld, sessionModules.size());
        }
        
        return totalCleaned;
    }
    
    /**
     * Obtiene estadísticas de conexiones WebSocket
     */
    public Map<String, Object> getConnectionStats() {
        int total = sessionModules.size();
        int inactive = 0;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(INACTIVITY_TIMEOUT_MINUTES);
        
        for (LocalDateTime lastActivity : sessionLastActivity.values()) {
            if (lastActivity == null || lastActivity.isBefore(cutoff)) {
                inactive++;
            }
        }
        
        return Map.of(
            "total", total,
            "active", total - inactive,
            "inactive", inactive,
            "maxConnections", MAX_CONNECTIONS
        );
    }

    public record SessionContext(
            boolean device,
            String role,
            String deviceType,
            Long sedeId,
            Long moduleId,
            Instant tokenExpiresAt) {}
    
}
