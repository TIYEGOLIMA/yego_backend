package com.yego.backend.scheduler.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverKpiResponse;
import com.yego.backend.handler.yego_pro_ops.FleetDriverNotificationHandler;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FleetDriverScheduler {
    
    private final FleetDriverService fleetDriverService;
    private final FleetDriverNotificationHandler notificationHandler;
    
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void consultarConductoresPeriodicamente() {
        try {
            DriverKpiResponse kpis = fleetDriverService.consultarConductores();
            notificationHandler.enviarKpisActualizados(kpis);
        } catch (Exception e) {
            log.error("❌ Error en consulta periódica: {}", e.getMessage());
        }
    }
}

