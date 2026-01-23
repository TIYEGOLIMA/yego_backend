package com.yego.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuración del TaskScheduler para WebSocket heartbeat
 * Clase separada para asegurar que se inicialice antes del WebSocketConfig
 * @Order(1) asegura que se ejecute antes que otras configuraciones
 */
@Slf4j
@Configuration
@Order(1)
public class WebSocketTaskSchedulerConfig {
    
    /**
     * Bean de TaskScheduler para el heartbeat de WebSocket
     * Spring lo usará automáticamente para el SimpleBroker
     * @Primary asegura que este sea el TaskScheduler usado por defecto
     * El nombre del bean debe ser "taskScheduler" para que Spring lo detecte automáticamente
     */
    @Bean(name = "taskScheduler")
    @Primary
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        log.info("✅ [WebSocketTaskScheduler] TaskScheduler configurado para heartbeat (bean name: taskScheduler)");
        return scheduler;
    }
}

