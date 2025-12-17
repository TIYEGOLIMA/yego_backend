package com.yego.backend.handler.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverKpiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FleetDriverNotificationHandler {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void enviarKpisActualizados(DriverKpiResponse kpis) {
        try {
            messagingTemplate.convertAndSend("/topic/pro-ops/kpis", kpis);
            log.debug("📤 KPIs enviados por WebSocket: viajeActivo={}, noDisponibles={}, disponibles={}, sinGPS={}", 
                kpis.getViajeActivo(), kpis.getNoDisponibles(), kpis.getDisponibles(), kpis.getSinGPS());
        } catch (Exception e) {
            log.error("❌ Error enviando KPIs por WebSocket: {}", e.getMessage());
        }
    }
}

