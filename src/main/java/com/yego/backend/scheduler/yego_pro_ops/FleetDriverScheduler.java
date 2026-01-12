package com.yego.backend.scheduler.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverKpiResponse;
import com.yego.backend.handler.yego_pro_ops.FleetDriverNotificationHandler;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FleetDriverScheduler {
    
    private final FleetDriverService fleetDriverService;
    private final FleetDriverNotificationHandler notificationHandler;
    private final WebSocketSessionService webSocketSessionService;
    
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void consultarConductoresPeriodicamente() {
        // Verificar si hay usuarios con acceso al módulo pro-ops antes de procesar
        Set<String> sessionsWithAccess = webSocketSessionService.getSessionsWithModuleAccess("pro-ops");
        if (sessionsWithAccess.isEmpty()) {
            log.debug("⏭️ [FleetDriverScheduler] No hay usuarios con acceso a pro-ops - omitiendo consulta de KPIs");
            return;
        }
        
        try {
            DriverKpiResponse kpis = fleetDriverService.consultarConductores();
            notificationHandler.enviarKpisActualizados(kpis);
        } catch (Exception e) {
            log.error("❌ Error en consulta periódica: {}", e.getMessage());
        }
    }
}

