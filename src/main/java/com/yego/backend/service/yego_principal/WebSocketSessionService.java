package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class WebSocketSessionService {
    
    private final Map<String, List<ModuleResponse>> sessionModules = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserIds = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    
    public void saveUserModules(String sessionId, List<ModuleResponse> modules, String userId) {
        if (sessionId != null && modules != null) {
            sessionModules.put(sessionId, modules);
            if (userId != null) {
                sessionUserIds.put(sessionId, userId);
            }
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
        }
    }
    
}

