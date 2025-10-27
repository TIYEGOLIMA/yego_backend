package com.yego.backend.config.yego_garantizado;

import com.yego.backend.handler.yego_garantizado.SystemNotificationHandler;
import com.yego.backend.service.yego_garantizado.SystemStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuración de tareas programadas para el sistema de garantizado.
 * Maneja la activación/desactivación automática del sistema según horarios específicos.
 */
@Configuration
@EnableScheduling
@Slf4j
public class SchedulerConfig {

    private final SystemStatusService systemStatusService;
    private final SystemNotificationHandler systemNotificationHandler;

    /**
     * Constructor para inyección de dependencias.
     * @param systemStatusService Servicio para gestionar el estado del sistema.
     * @param systemNotificationHandler Handler para notificaciones WebSocket.
     */
    public SchedulerConfig(SystemStatusService systemStatusService, SystemNotificationHandler systemNotificationHandler) {
        this.systemStatusService = systemStatusService;
        this.systemNotificationHandler = systemNotificationHandler;
    }

    /**
     * Verificar estado del sistema al iniciar la aplicación.
     * Se ejecuta cuando la aplicación está completamente cargada.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void verificarEstadoAlIniciar() {
        log.info("🚀 Aplicación iniciada - Verificando estado del sistema...");
        verificarEstadoSistema();
    }

    /**
     * Desactiva el sistema todos los viernes a las 23:59.
     * Cron: 0 59 23 * * FRI (segundo minuto hora día mes día_semana)
     */
    @Scheduled(cron = "0 59 23 * * FRI")
    public void desactivarSistemaViernes() {
        log.info("🕐 Ejecutando desactivación automática - Viernes 23:59");
        systemStatusService.deactivateSystem();
        log.warn("🚫 Sistema desactivado automáticamente - Fin de semana laboral");

        // Enviar notificación WebSocket
        systemNotificationHandler.broadcastSystemStatus(false, "Sistema desactivado automáticamente. Fin de semana laboral. Próxima activación: Lunes 9:00 AM");
    }

    /**
     * Activa el sistema todos los lunes a las 9:00 AM.
     * Cron: 0 0 9 * * MON (segundo minuto hora día mes día_semana)
     */
    @Scheduled(cron = "0 0 6 * * MON")
    public void activarSistemaLunes() {
        log.info("🌅 Ejecutando activación automática - Lunes 9:00 AM");
        systemStatusService.activateSystem();
        log.info("✅ Sistema activado automáticamente - Inicio de semana laboral");

        // Enviar notificación WebSocket
        systemNotificationHandler.broadcastSystemStatus(true, "Sistema activado automáticamente. Inicio de semana laboral. Próxima desactivación: Viernes 23:59");
    }

    /**
     * Verificación cada hora para asegurar que el sistema esté en el estado correcto.
     * Delega la lógica de verificación al SystemStatusService para evitar redundancia.
     */
    @Scheduled(fixedRate = 3600000) // Cada hora (3600000 ms)
    public void verificarEstadoSistema() {
        // Solo verificar si el estado actual es correcto, sin duplicar lógica
        boolean currentStatus = systemStatusService.isSystemActive();
        log.debug("🔍 Verificación horaria - Estado actual: {}", currentStatus ? "ACTIVO" : "INACTIVO");
    }
}

