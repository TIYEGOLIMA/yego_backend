package com.yego.backend.scheduler;

import com.yego.backend.service.yego_garantizado.YegoGarantizadoRegistroService;
import com.yego.backend.service.WebSocketService;
import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class GarantizadoScheduler {

    private final YegoGarantizadoRegistroService yegoGarantizadoRegistroService;
    private final WebSocketService webSocketService;

    /**
     * Procesa automáticamente todos los conductores de la semana actual
     * cada lunes a las 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void procesarConductoresSemanaActual() {
        try {
            // Obtener la semana actual
            String semanaActual = obtenerSemanaActual();
            
            log.info("[GarantizadoScheduler] Iniciando procesamiento automático - Lunes 9:00 AM");
            log.info("[GarantizadoScheduler] Semana actual: {}", semanaActual);
            
            // Procesar todos los conductores de la semana actual y obtener datos completos
            GarantizadoListResponse datosCompletos = yegoGarantizadoRegistroService.procesarYDevolverSemanaActual();
            
            log.info(" [GarantizadoScheduler] Procesamiento automático completado");
            log.info("[GarantizadoScheduler] Total de conductores procesados: {}", datosCompletos.getConductores().size());
            
            // Enviar datos completos para actualizar la tabla
            webSocketService.enviarDatosCompletosGarantizado(
                datosCompletos.getConductores(),
                datosCompletos.getSemanaActual()
            );
            
        } catch (Exception e) {
            log.error("[GarantizadoScheduler] Error en procesamiento automático: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene la semana actual en formato SEMANAXX
     */
    private String obtenerSemanaActual() {
        LocalDateTime ahora = LocalDateTime.now();
        int diaDelAnio = ahora.getDayOfYear();
        int semana = (diaDelAnio / 7) + 1;
        return "SEMANA" + semana;
    }
}
