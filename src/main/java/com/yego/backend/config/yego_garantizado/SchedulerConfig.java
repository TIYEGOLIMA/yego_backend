package com.yego.backend.config.yego_garantizado;

import com.yego.backend.handler.yego_garantizado.SystemNotificationHandler;
import com.yego.backend.service.yego_garantizado.CalculoGarantizadoService;
import com.yego.backend.service.yego_garantizado.ProcesoGarantizadoEstadoService;
import com.yego.backend.service.yego_garantizado.SystemStatusService;
import com.yego.backend.service.yego_garantizado.YegoGarantizadoRegistroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuración de tareas programadas para el sistema de garantizado.
 * Maneja la activación/desactivación automática del sistema según horarios específicos.
 * 
 * Nota: @EnableScheduling ya está habilitado en YegoBackendApplication
 */
@Configuration
@Profile("prod")
@Slf4j
@RequiredArgsConstructor
public class SchedulerConfig {

    private final SystemStatusService systemStatusService;
    private final SystemNotificationHandler systemNotificationHandler;
    private final CalculoGarantizadoService calculoGarantizadoService;
    private final ProcesoGarantizadoEstadoService procesoGarantizadoEstadoService;
    private final YegoGarantizadoRegistroService yegoGarantizadoRegistroService;

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
     * Notifica que el botón de procesamiento está disponible
     * Se ejecuta todos los lunes a las 9:00 AM (hora de Lima)
     * Desbloquea el botón y notifica al frontend
     * Cron: 0 0 9 * * MON (segundo minuto hora día mes día_semana)
     */
    @Scheduled(cron = "0 0 9 * * MON", zone = "America/Lima")
    public void notificarBotónDisponible() {
        try {
            log.info("🔔 [SchedulerConfig] Ejecutando notificación de botón disponible - Lunes 9:00 AM");
            
            // Notificar el estado actual (debería estar desbloqueado si es lunes 9 AM)
            procesoGarantizadoEstadoService.notificarEstadoActual();
            
            log.info("✅ [SchedulerConfig] Notificación de botón disponible enviada");
            
        } catch (Exception e) {
            log.error("❌ [SchedulerConfig] Error notificando botón disponible: {}", e.getMessage(), e);
        }
    }

    /**
     * Procesamiento automático de garantizado
     * Se ejecuta todos los lunes a las 11:00 AM (hora de Lima)
     * FLUJO:
     * 1. Bloquear el botón PRIMERO
     * 2. Copiar configuraciones de la semana anterior
     * 3. Ejecutar el procesamiento automático
     * Cron: 0 0 11 * * MON (segundo minuto hora día mes día_semana)
     */
    @Scheduled(cron = "0 0 11 * * MON", zone = "America/Lima")
    public void procesamientoAutomaticoGarantizado() {
        try {
            log.info("🔄 [SchedulerConfig] ===== INICIANDO PROCESAMIENTO AUTOMÁTICO DE GARANTIZADO - Lunes 11:00 AM =====");
            
            // PASO 1: Bloquear el botón PRIMERO (antes de procesar)
            log.info("🔒 [SchedulerConfig] PASO 1: Bloqueando botón de procesamiento...");
            procesoGarantizadoEstadoService.registrarProcesamiento();
            log.info("✅ [SchedulerConfig] Botón bloqueado exitosamente");
            
            // PASO 2: Copiar configuraciones de la semana anterior a la semana actual
            log.info("📋 [SchedulerConfig] PASO 2: Copiando configuraciones de semana anterior...");
            calculoGarantizadoService.copiarConfiguracionesAutomaticamente();
            log.info("✅ [SchedulerConfig] Configuraciones copiadas exitosamente");
            
            // PASO 3: Ejecutar el procesamiento automático de la semana anterior
            log.info("🚀 [SchedulerConfig] PASO 3: Iniciando procesamiento automático de semana anterior...");
            yegoGarantizadoRegistroService.procesarYDevolverSemanaAnterior();
            log.info("✅ [SchedulerConfig] ===== PROCESAMIENTO AUTOMÁTICO COMPLETADO EXITOSAMENTE =====");
            
        } catch (Exception e) {
            log.error("❌ [SchedulerConfig] Error ejecutando procesamiento automático: {}", e.getMessage(), e);
        }
    }
}
