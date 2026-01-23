package com.yego.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuración del TaskScheduler para WebSocket heartbeat
 * Clase separada para asegurar que se inicialice antes del WebSocketConfig
 */
@Slf4j
@Configuration
public class WebSocketTaskSchedulerConfig {
    
    /**
     * Bean de TaskScheduler para el heartbeat de WebSocket
     * Spring lo usará automáticamente para el SimpleBroker
     * @Primary asegura que este sea el TaskScheduler usado por defecto
     */
    @Bean
    @Primary
    public TaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        log.info("✅ [WebSocketTaskScheduler] TaskScheduler configurado para heartbeat");
        return scheduler;
    }
}

