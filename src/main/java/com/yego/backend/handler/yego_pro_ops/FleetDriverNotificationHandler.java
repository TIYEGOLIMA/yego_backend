package com.yego.backend.handler.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MultipleDriversTripsSimplifiedResponse;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yego.backend.service.yego_principal.FilteredWebSocketService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.annotation.PreDestroy;

@Slf4j
@Component
@RequiredArgsConstructor
public class FleetDriverNotificationHandler {
    
    private final FilteredWebSocketService filteredWebSocketService;
    private final WebSocketSessionService webSocketSessionService;
    private final DriverOrdersService driverOrdersService;
    
    // Executor para procesamiento asíncrono de viajes simplificados
    // Pool pequeño (3 threads) ya que solo procesa cuando hay conductores en orden (cada 10 segundos)
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("⚠️ [FleetDriverNotificationHandler] ExecutorService no terminó en 30 segundos, forzando cierre");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("❌ [FleetDriverNotificationHandler] Error cerrando ExecutorService", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Envía datos de conductores con status "in_order"
     * Se llama desde el scheduler que se ejecuta cada 10 segundos
     * También obtiene y envía automáticamente los viajes simplificados de estos conductores
     */
    public void enviarConductoresEnOrden(DriversInOrderResponse response) {
        try {
            // Verificar si hay usuarios con acceso al módulo pro-ops antes de enviar
            Set<String> sessionsWithAccess = webSocketSessionService.getSessionsWithModuleAccess("pro-ops");
            if (sessionsWithAccess.isEmpty()) {
                log.debug("⏭️ [FleetDriverNotificationHandler] No hay usuarios con acceso a pro-ops - omitiendo envío de conductores en orden");
                return;
            }
            
            log.debug("🚗 [FleetDriverNotificationHandler] Enviando conductores en orden - {} conductores", response.getTotal());
            
            Map<String, Object> data = new HashMap<>();
            data.put("type", "DRIVERS_IN_ORDER_UPDATE");
            data.put("conductores", response.getConductores());
            data.put("total", response.getTotal());
            data.put("timestamp", LocalDateTime.now().toString());
            
            // Enviar a topic específico de conductores en orden
            filteredWebSocketService.convertAndSend("/topic/pro-ops/conductores-en-orden", data);
            
            log.debug("✅ [FleetDriverNotificationHandler] Conductores en orden enviados por WebSocket - {} conductores", response.getTotal());
            
            // Obtener y enviar viajes simplificados de los conductores en orden
            enviarViajesSimplificadosEnCurso(response);
            
        } catch (Exception e) {
            log.error("❌ [FleetDriverNotificationHandler] Error enviando conductores en orden por WebSocket: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene los viajes simplificados de los conductores en orden y los envía por WebSocket
     * Se ejecuta automáticamente cada vez que se actualizan los conductores en orden (cada 5 minutos)
     * Procesamiento asíncrono para no bloquear el scheduler
     */
    private void enviarViajesSimplificadosEnCurso(DriversInOrderResponse response) {
        try {
            if (response == null || response.getConductores() == null || response.getConductores().isEmpty()) {
                log.info("⏭️ [FleetDriverNotificationHandler] No hay conductores en orden - omitiendo envío de viajes simplificados");
                return;
            }
            
            // Extraer driver_ids de los conductores en orden
            List<String> driverIds = response.getConductores().stream()
                .map(conductor -> conductor.getId())
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());
            
            if (driverIds.isEmpty()) {
                log.info("⏭️ [FleetDriverNotificationHandler] No hay driver_ids válidos - omitiendo envío de viajes simplificados");
                return;
            }
            
            log.info("📋 [FleetDriverNotificationHandler] Iniciando obtención de viajes simplificados para {} conductores (WebSocket cada 5 minutos)", driverIds.size());
            
            // 1. ENVIAR MENSAJE DE "PROCESANDO" AL FRONTEND
            Map<String, Object> loadingData = new HashMap<>();
            loadingData.put("type", "VIAJES_SIMPLIFICADOS_EN_CURSO_LOADING");
            loadingData.put("status", "processing");
            loadingData.put("message", "Obteniendo viajes simplificados de conductores en curso...");
            loadingData.put("total_drivers", driverIds.size());
            loadingData.put("timestamp", LocalDateTime.now().toString());
            
            filteredWebSocketService.convertAndSend("/topic/pro-ops/viajes-simplificados-en-curso", loadingData);
            log.info("⏳ [FleetDriverNotificationHandler] Enviado mensaje de carga por WebSocket - procesando {} conductores", driverIds.size());
            
            // 2. PROCESAR DE FORMA ASÍNCRONA
            CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("📋 [FleetDriverNotificationHandler] Obteniendo viajes simplificados para {} conductores en curso (ejecutado cada 5 minutos)", driverIds.size());
                    
                    // Obtener rango de fechas del día actual (zona horaria de Lima)
                    ZoneId limaZone = ZoneId.of("America/Lima");
                    LocalDate fechaHoy = LocalDate.now(limaZone);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                    
                    String dateFrom = fechaHoy.atStartOfDay().atZone(limaZone).format(formatter);
                    String dateTo = fechaHoy.atTime(23, 59, 59).atZone(limaZone).format(formatter);
                    
                    // Obtener viajes simplificados
                    return driverOrdersService.obtenerViajesSimplificadosMultiples(driverIds, dateFrom, dateTo);
                    
                } catch (Exception e) {
                    log.error("❌ [FleetDriverNotificationHandler] Error obteniendo viajes simplificados: {}", e.getMessage(), e);
                    return null;
                }
            }, executorService).thenAccept(viajesSimplificados -> {
                try {
                    if (viajesSimplificados == null || viajesSimplificados.getDrivers() == null) {
                        log.debug("⏭️ [FleetDriverNotificationHandler] No se obtuvieron viajes simplificados");
                        
                        // Enviar mensaje de error o vacío
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("type", "VIAJES_SIMPLIFICADOS_EN_CURSO_UPDATE");
                        errorData.put("status", "error");
                        errorData.put("message", "No se pudieron obtener viajes simplificados");
                        errorData.put("drivers", new java.util.ArrayList<>());
                        errorData.put("total_drivers", 0);
                        errorData.put("timestamp", LocalDateTime.now().toString());
                        
                        filteredWebSocketService.convertAndSend("/topic/pro-ops/viajes-simplificados-en-curso", errorData);
                        return;
                    }
                    
                    // 3. ENVIAR RESULTADO AL FRONTEND
                    Map<String, Object> data = new HashMap<>();
                    data.put("type", "VIAJES_SIMPLIFICADOS_EN_CURSO_UPDATE");
                    data.put("status", "completed");
                    data.put("date_from", viajesSimplificados.getDateFrom());
                    data.put("date_to", viajesSimplificados.getDateTo());
                    data.put("drivers", viajesSimplificados.getDrivers());
                    data.put("total_drivers", viajesSimplificados.getDrivers().size());
                    data.put("timestamp", LocalDateTime.now().toString());
                    
                    // Enviar a topic específico de viajes simplificados en curso
                    filteredWebSocketService.convertAndSend("/topic/pro-ops/viajes-simplificados-en-curso", data);
                    
                    log.info("✅ [FleetDriverNotificationHandler] Viajes simplificados en curso enviados por WebSocket - {} conductores (próxima ejecución en 5 minutos)", 
                        viajesSimplificados.getDrivers().size());
                    
                } catch (Exception e) {
                    log.error("❌ [FleetDriverNotificationHandler] Error enviando viajes simplificados: {}", e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            log.error("❌ [FleetDriverNotificationHandler] Error en procesamiento asíncrono de viajes simplificados: {}", e.getMessage(), e);
        }
    }
}

