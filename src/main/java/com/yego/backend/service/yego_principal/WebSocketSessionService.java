package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WebSocketSessionService {
    
    // Límite máximo de conexiones WebSocket simultáneas
    private static final int MAX_CONNECTIONS = 500;
    
    // Timeout de inactividad: 5 horas sin actividad = conexión muerta
    private static final int INACTIVITY_TIMEOUT_MINUTES = 300; // 5 horas
    
    private final Map<String, List<ModuleResponse>> sessionModules = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserIds = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> sessionLastActivity = new ConcurrentHashMap<>();
    
    public void saveUserModules(String sessionId, List<ModuleResponse> modules, String userId) {
        if (sessionId != null && modules != null) {
            // Verificar límite de conexiones
            int currentConnections = sessionModules.size();
            if (currentConnections >= MAX_CONNECTIONS) {
                log.warn("⚠️ [WebSocket] Límite de conexiones alcanzado: {}/{}", currentConnections, MAX_CONNECTIONS);
                // Limpiar conexiones inactivas antes de rechazar
                cleanupInactiveSessions();
                // Si aún está lleno, rechazar nueva conexión
                if (sessionModules.size() >= MAX_CONNECTIONS) {
                    log.error("❌ [WebSocket] Rechazando nueva conexión: límite alcanzado");
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
            log.debug("🔌 [WebSocket] Sesión {} removida (Total: {})", sessionId, sessionModules.size());
        }
    }
    
    /**
     * Limpia sesiones inactivas (sin actividad por más de 5 horas)
     * @return Número de sesiones limpiadas
     */
    public int cleanupInactiveSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(INACTIVITY_TIMEOUT_MINUTES);
        int cleaned = 0;
        
        for (Map.Entry<String, LocalDateTime> entry : sessionLastActivity.entrySet()) {
            String sessionId = entry.getKey();
            LocalDateTime lastActivity = entry.getValue();
            
            if (lastActivity == null || lastActivity.isBefore(cutoff)) {
                removeSession(sessionId);
                cleaned++;
            }
        }
        
        if (cleaned > 0) {
            log.info("🧹 [WebSocket] Limpiadas {} sesiones inactivas (sin actividad > 5 horas) (Total activas: {})", cleaned, sessionModules.size());
        }
        
        return cleaned;
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
    
}

