package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para mapear topics de WebSocket a módulos del sistema
 */
@Service
public class WebSocketModuleMappingService {
    
    /**
     * Determina qué módulo corresponde a un topic de WebSocket
     * 
     * @param topic Topic de WebSocket (ej: /topic/pro-ops/kpis)
     * @param userModules Lista de módulos del usuario
     * @return Módulo correspondiente al topic, o Optional.empty() si no hay coincidencia
     */
    public Optional<ModuleResponse> getModuleForTopic(String topic, List<ModuleResponse> userModules) {
        if (topic == null || userModules == null || userModules.isEmpty()) {
            return Optional.empty();
        }
        
        // Normalizar el topic (remover prefijo /topic si existe)
        String normalizedTopic = topic.startsWith("/topic/") 
            ? topic.substring(7) 
            : topic.startsWith("topic/") 
                ? topic.substring(6) 
                : topic;
        
        // Mapeo de topics a patrones de módulos
        final String modulePattern;
        
        if (normalizedTopic.startsWith("pro-ops/") || normalizedTopic.startsWith("pro-ops")) {
            // /topic/pro-ops/* → Módulo con URL que contiene "yego-pro-ops" o "pro-ops"
            modulePattern = "pro-ops";
        } else if (normalizedTopic.startsWith("garantizado/") || normalizedTopic.startsWith("garantizado")) {
            // /topic/garantizado/* → Módulo con URL "/garantizado"
            modulePattern = "garantizado";
        } else if (normalizedTopic.startsWith("ticketera/") || normalizedTopic.startsWith("ticketera") 
                || normalizedTopic.startsWith("tickets/") || normalizedTopic.startsWith("tickets")
                || normalizedTopic.startsWith("ticket-") || normalizedTopic.startsWith("modulos-atencion")) {
            // /topic/ticketera/*, /topic/tickets/*, /topic/modulos-atencion → Módulo con URL "/tickets"
            modulePattern = "tickets";
        } else if (normalizedTopic.startsWith("sistemas-externos/") || normalizedTopic.startsWith("sistemas-externos")) {
            // /topic/sistemas-externos/* → Módulo con URL "/sistemas-externos"
            modulePattern = "sistemas-externos";
        } else {
            // Topics del sistema (/topic/system, /topic/user/*) no requieren módulo específico
            return Optional.empty();
        }
        
        final String finalPattern = modulePattern.toLowerCase();
        
        // Buscar módulo que coincida con el patrón
        return userModules.stream()
            .filter(module -> {
                String url = module.getUrl() != null ? module.getUrl().toLowerCase() : "";
                String nombre = module.getNombre() != null ? module.getNombre().toLowerCase() : "";
                
                // Verificar si la URL o el nombre contiene el patrón
                return url.contains(finalPattern) 
                    || nombre.contains(finalPattern);
            })
            .findFirst();
    }
    
    /**
     * Verifica si un usuario tiene acceso a un topic específico
     * 
     * @param topic Topic de WebSocket
     * @param userModules Lista de módulos del usuario
     * @return true si el usuario tiene acceso, false en caso contrario
     */
    public boolean hasAccessToTopic(String topic, List<ModuleResponse> userModules) {
        // Topics del sistema siempre están permitidos
        if (topic == null) {
            return false;
        }
        
        String normalizedTopic = topic.startsWith("/topic/") 
            ? topic.substring(7) 
            : topic.startsWith("topic/") 
                ? topic.substring(6) 
                : topic;
        
        // Topics del sistema (/topic/system, /topic/user/*) no requieren módulo
        if (normalizedTopic.startsWith("system") || normalizedTopic.startsWith("user/")) {
            return true;
        }
        
        // Para otros topics, verificar acceso al módulo
        return getModuleForTopic(topic, userModules).isPresent();
    }
}

