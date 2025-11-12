package com.yego.backend.config.yego_garantizado;

import com.yego.backend.handler.yego_garantizado.SystemNotificationHandler;
import com.yego.backend.service.yego_garantizado.CalculoGarantizadoService;
import com.yego.backend.service.yego_garantizado.SystemStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
    private final CalculoGarantizadoService calculoGarantizadoService;

    /**
     * Constructor para inyección de dependencias.
     * @param systemStatusService Servicio para gestionar el estado del sistema.
     * @param systemNotificationHandler Handler para notificaciones WebSocket.
     * @param calculoGarantizadoService Servicio para gestionar cálculos de garantizado.
     */
    public SchedulerConfig(SystemStatusService systemStatusService, 
                          SystemNotificationHandler systemNotificationHandler,
                          CalculoGarantizadoService calculoGarantizadoService) {
        this.systemStatusService = systemStatusService;
        this.systemNotificationHandler = systemNotificationHandler;
        this.calculoGarantizadoService = calculoGarantizadoService;
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
        log.info("🌅 Ejecutando activación automática - Lunes 6:00 AM");
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

    /**
     * Copia automáticamente las configuraciones de la semana anterior a la semana actual
     * todos los lunes desde las 8:00 AM hasta las 8:50 AM (cada 10 minutos)
     * Solo se ejecuta si no existen configuraciones para la semana actual
     * Cron: 0 0,10,20,30,40,50 8 * * MON (segundo minuto hora día mes día_semana)
     */
    @Scheduled(cron = "0 0,10,20,30,40,50 8 * * MON", zone = "America/Lima")
    public void copiarConfiguracionesAutomaticamenteLunes() {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            DayOfWeek diaActual = ahora.getDayOfWeek();
            LocalTime horaActual = ahora.toLocalTime();
            
            // Verificar que sea lunes y esté entre las 8:00 AM y las 8:50 AM
            if (diaActual != DayOfWeek.MONDAY) {
                log.debug("⏭️ [SchedulerConfig] No es lunes, omitiendo copia automática de configuraciones");
                return;
            }
            
            if (horaActual.isBefore(LocalTime.of(8, 0)) || horaActual.isAfter(LocalTime.of(8, 50))) {
                log.debug("⏭️ [SchedulerConfig] Fuera del horario permitido (8:00-8:50 AM), omitiendo copia automática");
                return;
            }
            
            log.info("🔄 [SchedulerConfig] Ejecutando copia automática de configuraciones de semana anterior (Lunes {} - {})", 
                horaActual.toString(), ahora.toLocalDate().toString());
            
            calculoGarantizadoService.copiarConfiguracionesAutomaticamente();
            
            log.info("✅ [SchedulerConfig] Copia automática de configuraciones completada");
            
        } catch (Exception e) {
            log.error("❌ [SchedulerConfig] Error ejecutando copia automática de configuraciones: {}", e.getMessage(), e);
        }
    }
}

