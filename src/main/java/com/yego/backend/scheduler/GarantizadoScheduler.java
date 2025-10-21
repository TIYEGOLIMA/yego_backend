package com.yego.backend.scheduler;

import com.yego.backend.service.yego_garantizado.YegoGarantizadoRegistroService;
import com.yego.backend.service.WebSocketService;
import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class GarantizadoScheduler {

    private final YegoGarantizadoRegistroService yegoGarantizadoRegistroService;
    private final WebSocketService webSocketService;

    /**
     * Procesa automáticamente todos los conductores de la semana anterior
     * todos los martes a las 11:16 AM
     */
    @Scheduled(cron = "0 16 11 * * TUE")
    public void procesarConductoresSemanaAnterior() {
        try {
            // Obtener la semana anterior (la que se completó el domingo)
            String semanaAnterior = obtenerSemanaAnterior();
            
            log.info("[GarantizadoScheduler] Iniciando procesamiento automático - Martes 11:16 AM");
            log.info("[GarantizadoScheduler] Procesando semana anterior: {}", semanaAnterior);
            
            // Procesar todos los conductores de la semana anterior y obtener datos completos
            GarantizadoListResponse datosCompletos = yegoGarantizadoRegistroService.procesarYDevolverSemanaAnterior();
            
            log.info(" [GarantizadoScheduler] Procesamiento automático completado");
            log.info("[GarantizadoScheduler] Total de conductores procesados: {}", datosCompletos.getConductores().size());
            
            // Enviar datos completos para actualizar la tabla
            webSocketService.enviarDatosCompletosGarantizado(
                datosCompletos.getConductores(),
                datosCompletos.getSemanaAnterior()
            );
            
        } catch (Exception e) {
            log.error("[GarantizadoScheduler] Error en procesamiento automático: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene la semana anterior en formato SEMANAXX
     * (la semana que se completó el domingo anterior)
     */
    private String obtenerSemanaAnterior() {
        LocalDateTime ahora = LocalDateTime.now();
        int semanaActual = ahora.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        int semanaAnterior = semanaActual - 1;
        
        // Si estamos en la semana 1, la semana anterior es la última semana del año anterior
        if (semanaAnterior <= 0) {
            semanaAnterior = 52; // Asumimos que el año anterior tenía 52 semanas
        }
        
        return "SEMANA" + semanaAnterior;
    }

}
