package com.yego.backend.handler.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.AllDriversOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverKpiResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yego.backend.service.yego_principal.FilteredWebSocketService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FleetDriverNotificationHandler {
    
    private final FilteredWebSocketService filteredWebSocketService;
    private final WebSocketSessionService webSocketSessionService;
    
    public void enviarKpisActualizados(DriverKpiResponse kpis) {
        try {
            // Verificar si hay usuarios con acceso al módulo pro-ops antes de enviar
            Set<String> sessionsWithAccess = webSocketSessionService.getSessionsWithModuleAccess("pro-ops");
            if (sessionsWithAccess.isEmpty()) {
                log.debug("⏭️ [FleetDriverNotificationHandler] No hay usuarios con acceso a pro-ops - omitiendo envío de KPIs");
                return;
            }
            
            filteredWebSocketService.convertAndSend("/topic/pro-ops/kpis", kpis);
            log.debug("📤 KPIs enviados por WebSocket: viajeActivo={}, noDisponibles={}, disponibles={}, sinGPS={}", 
                kpis.getViajeActivo(), kpis.getNoDisponibles(), kpis.getDisponibles(), kpis.getSinGPS());
        } catch (Exception e) {
            log.error("❌ Error enviando KPIs por WebSocket: {}", e.getMessage());
        }
    }
    
    /**
     * Envía datos de todos los conductores con sus viajes en vivo
     * Se llama desde el scheduler que se ejecuta cada 5 minutos
     */
    public void enviarConductoresConViajes(AllDriversOrdersResponse response) {
        try {
            // Verificar si hay usuarios con acceso al módulo pro-ops antes de enviar
            Set<String> sessionsWithAccess = webSocketSessionService.getSessionsWithModuleAccess("pro-ops");
            if (sessionsWithAccess.isEmpty()) {
                log.debug("⏭️ [FleetDriverNotificationHandler] No hay usuarios con acceso a pro-ops - omitiendo envío de conductores con viajes");
                return;
            }
            
            log.info("🚗 [FleetDriverNotificationHandler] Enviando datos de conductores con viajes - {} conductores para fecha {}", 
                response.getTotalConductores(), response.getFecha());
            
            Map<String, Object> data = new HashMap<>();
            data.put("type", "DRIVERS_ORDERS_UPDATE");
            data.put("fecha", response.getFecha());
            data.put("conductores", response.getConductores());
            data.put("totalConductores", response.getTotalConductores());
            data.put("timestamp", LocalDateTime.now().toString());
            
            // Enviar a topic específico de pro-ops conductores con viajes
            filteredWebSocketService.convertAndSend("/topic/pro-ops/conductores-viajes", data);
            
            // También enviar al topic general del sistema
            filteredWebSocketService.convertAndSend("/topic/system", data);
            
            log.info("✅ [FleetDriverNotificationHandler] Datos de conductores con viajes enviados por WebSocket - {} conductores", 
                response.getTotalConductores());
        } catch (Exception e) {
            log.error("❌ [FleetDriverNotificationHandler] Error enviando conductores con viajes por WebSocket: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía datos de conductores con status "in_order"
     * Se llama desde el scheduler que se ejecuta cada 10 segundos
     */
    public void enviarConductoresEnOrden(DriversInOrderResponse response) {
        try {
            // Verificar si hay usuarios con acceso al módulo pro-ops antes de enviar
            Set<String> sessionsWithAccess = webSocketSessionService.getSessionsWithModuleAccess("pro-ops");
            if (sessionsWithAccess.isEmpty()) {
                log.debug("⏭️ [FleetDriverNotificationHandler] No hay usuarios con acceso a pro-ops - omitiendo envío de conductores en orden");
                return;
            }
            
            log.debug("🚗 [FleetDriverNotificationHandler] Enviando conductores en orden - {} conductores", response.getTotal());
            
            Map<String, Object> data = new HashMap<>();
            data.put("type", "DRIVERS_IN_ORDER_UPDATE");
            data.put("conductores", response.getConductores());
            data.put("total", response.getTotal());
            data.put("timestamp", LocalDateTime.now().toString());
            
            // Enviar a topic específico de conductores en orden
            filteredWebSocketService.convertAndSend("/topic/pro-ops/conductores-en-orden", data);
            
            log.debug("✅ [FleetDriverNotificationHandler] Conductores en orden enviados por WebSocket - {} conductores", response.getTotal());
        } catch (Exception e) {
            log.error("❌ [FleetDriverNotificationHandler] Error enviando conductores en orden por WebSocket: {}", e.getMessage(), e);
        }
    }
}

