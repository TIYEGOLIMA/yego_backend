package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.request.*;
import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoListResponse;
import com.yego.backend.entity.yego_garantizado.entities.CalculoGarantizado;
import com.yego.backend.repository.yego_garantizado.CalculoGarantizadoRepository;
import com.yego.backend.handler.yego_garantizado.SystemNotificationHandler;
import com.yego.backend.service.yego_garantizado.CalculoGarantizadoService;
import com.yego.backend.service.yego_garantizado.YegoGarantizadoRegistroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de cálculos de garantizado
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculoGarantizadoServiceImpl implements CalculoGarantizadoService {

    private final CalculoGarantizadoRepository calculoRepository;
    private final YegoGarantizadoRegistroService yegoGarantizadoRegistroService;
    private final SystemNotificationHandler systemNotificationHandler;
    private final com.yego.backend.service.yego_garantizado.ProcesoGarantizadoEstadoService procesoEstadoService;

    @Override
    @Transactional
    public void guardarConfiguraciones(CalcularGarantizadoRequest request) {
        // Si no viene semana en el request, calcular la semana anterior automáticamente
        if (request.getSemana() == null || request.getSemana().isEmpty()) {
            String semanaAnterior = obtenerSemanaAnterior();
            log.info("📊 [CalculoGarantizadoService] No se envió semana, usando semana anterior: {}", semanaAnterior);
            request.setSemana(semanaAnterior);
        } else {
            log.info("📊 [CalculoGarantizadoService] Guardando configuraciones para semana: {}", request.getSemana());
        }
        
        int totalGuardados = 0;
        
        // Iterar sobre cada país
        for (PaisCalculoRequest paisRequest : request.getPaises()) {
            String pais = paisRequest.getPais();
            
            // Iterar sobre cada ciudad del país
            for (CiudadCalculoRequest ciudadRequest : paisRequest.getCiudades()) {
                String ciudad = ciudadRequest.getCiudad();
                
                // Eliminar todos los registros existentes de esta ciudad/semana/pais antes de guardar los nuevos
                String paisLower = pais.toLowerCase();
                String ciudadLower = ciudad.toLowerCase();
                eliminarCalculosExistentes(paisLower, ciudadLower, request.getSemana());
                
                // Obtener listas de cálculos (pueden tener múltiples elementos)
                List<CalculoRequest> conBrandeoList = ciudadRequest.getConBrandeo() != null ? ciudadRequest.getConBrandeo() : new java.util.ArrayList<>();
                List<CalculoRequest> sinBrandeoList = ciudadRequest.getSinBrandeo() != null ? ciudadRequest.getSinBrandeo() : new java.util.ArrayList<>();
                
                // Guardar registros de CON BRANDEO (sinBrandeo debe estar en 0)
                for (CalculoRequest calcConBrandeo : conBrandeoList) {
                    GuardarCalculoGarantizadoRequest guardarRequest = crearRequestConBrandeo(
                        paisLower, ciudadLower, request.getSemana(), calcConBrandeo);
                    
                    guardarCalculoNuevo(guardarRequest);
                    totalGuardados++;
                    
                    log.info("✅ [CalculoGarantizadoService] Guardado CON BRANDEO - País: {}, Ciudad: {}, Viajes: {}, Garantizado: {}", 
                        pais, ciudad, guardarRequest.getViajesConBrandeo(), guardarRequest.getGarantizadoConBrandeo());
                }
                
                // Guardar registros de SIN BRANDEO (conBrandeo debe estar en 0)
                for (CalculoRequest calcSinBrandeo : sinBrandeoList) {
                    GuardarCalculoGarantizadoRequest guardarRequest = crearRequestSinBrandeo(
                        paisLower, ciudadLower, request.getSemana(), calcSinBrandeo);
                    
                    guardarCalculoNuevo(guardarRequest);
                    totalGuardados++;
                    
                    log.info("✅ [CalculoGarantizadoService] Guardado SIN BRANDEO - País: {}, Ciudad: {}, Viajes: {}, Garantizado: {}", 
                        pais, ciudad, guardarRequest.getViajesSinBrandeo(), guardarRequest.getGarantizadoSinBrandeo());
                }
            }
        }
        
        log.info("✅ [CalculoGarantizadoService] Guardadas {} configuraciones exitosamente", totalGuardados);
    }

    /**
     * Crear request para CON BRANDEO (sinBrandeo en 0)
     */
    private GuardarCalculoGarantizadoRequest crearRequestConBrandeo(String pais, String ciudad, String semana, 
                                                                      CalculoRequest calcConBrandeo) {
        GuardarCalculoGarantizadoRequest request = new GuardarCalculoGarantizadoRequest();
        request.setPais(pais);
        request.setCiudad(ciudad);
        request.setSemana(semana);
        // Valores CON BRANDEO
        request.setViajesConBrandeo(extraerValor(calcConBrandeo, CalculoRequest::getViajes, 0));
        request.setBonoConBrandeo(extraerBigDecimal(calcConBrandeo, CalculoRequest::getBono));
        request.setGarantizadoConBrandeo(extraerBigDecimal(calcConBrandeo, CalculoRequest::getGarantizado));
        request.setHorasConBrandeo(extraerValor(calcConBrandeo, CalculoRequest::getHoras, 0));
        // Valores SIN BRANDEO en 0
        request.setViajesSinBrandeo(0);
        request.setBonoSinBrandeo(BigDecimal.ZERO);
        request.setGarantizadoSinBrandeo(BigDecimal.ZERO);
        request.setHorasSinBrandeo(0);
        return request;
    }
    
    /**
     * Crear request para SIN BRANDEO (conBrandeo en 0)
     */
    private GuardarCalculoGarantizadoRequest crearRequestSinBrandeo(String pais, String ciudad, String semana, 
                                                                     CalculoRequest calcSinBrandeo) {
        GuardarCalculoGarantizadoRequest request = new GuardarCalculoGarantizadoRequest();
        request.setPais(pais);
        request.setCiudad(ciudad);
        request.setSemana(semana);
        // Valores CON BRANDEO en 0
        request.setViajesConBrandeo(0);
        request.setBonoConBrandeo(BigDecimal.ZERO);
        request.setGarantizadoConBrandeo(BigDecimal.ZERO);
        request.setHorasConBrandeo(0);
        // Valores SIN BRANDEO
        request.setViajesSinBrandeo(extraerValor(calcSinBrandeo, CalculoRequest::getViajes, 0));
        request.setBonoSinBrandeo(extraerBigDecimal(calcSinBrandeo, CalculoRequest::getBono));
        request.setGarantizadoSinBrandeo(extraerBigDecimal(calcSinBrandeo, CalculoRequest::getGarantizado));
        request.setHorasSinBrandeo(extraerValor(calcSinBrandeo, CalculoRequest::getHoras, 0));
        return request;
    }
    
    /**
     * Extraer valor Integer de un CalculoRequest
     */
    private Integer extraerValor(CalculoRequest calculo, java.util.function.Function<CalculoRequest, Integer> extractor, Integer defaultValue) {
        if (calculo == null) {
            return defaultValue;
        }
        Integer valor = extractor.apply(calculo);
        return valor != null ? valor : defaultValue;
    }
    
    /**
     * Extraer BigDecimal de un CalculoRequest (desde Integer)
     */
    private BigDecimal extraerBigDecimal(CalculoRequest calculo, java.util.function.Function<CalculoRequest, Integer> extractor) {
        if (calculo == null) {
            return BigDecimal.ZERO;
        }
        Integer valor = extractor.apply(calculo);
        return valor != null ? BigDecimal.valueOf(valor) : BigDecimal.ZERO;
    }
    
    /**
     * Eliminar todos los cálculos existentes para una ciudad/semana/pais
     */
    @Transactional
    private void eliminarCalculosExistentes(String pais, String ciudad, String semana) {
        List<CalculoGarantizado> existentes = calculoRepository.findAllByPaisAndCiudadAndSemana(pais, ciudad, semana);
        
        if (!existentes.isEmpty()) {
            log.info("🗑️ [CalculoGarantizadoService] Eliminando {} cálculos existentes para - País: {}, Ciudad: {}, Semana: {}", 
                existentes.size(), pais, ciudad, semana);
            calculoRepository.deleteAll(existentes);
        }
    }
    
    /**
     * Guardar un cálculo nuevo (sin verificar existencia, ya que se eliminan antes)
     */
    @Transactional
    private void guardarCalculoNuevo(GuardarCalculoGarantizadoRequest request) {
        log.info("💾 [CalculoGarantizadoService] Guardando nuevo cálculo - País: {}, Ciudad: {}, Semana: {}", 
            request.getPais(), request.getCiudad(), request.getSemana());
        
        CalculoGarantizado calculo = new CalculoGarantizado();
        calculo.setPais(request.getPais());
        calculo.setCiudad(request.getCiudad());
        calculo.setSemana(request.getSemana());
        calculo.setViajesConBrandeo(request.getViajesConBrandeo());
        calculo.setBonoConBrandeo(request.getBonoConBrandeo());
        calculo.setGarantizadoConBrandeo(request.getGarantizadoConBrandeo());
        calculo.setHorasConBrandeo(request.getHorasConBrandeo());
        calculo.setViajesSinBrandeo(request.getViajesSinBrandeo());
        calculo.setBonoSinBrandeo(request.getBonoSinBrandeo());
        calculo.setGarantizadoSinBrandeo(request.getGarantizadoSinBrandeo());
        calculo.setHorasSinBrandeo(request.getHorasSinBrandeo());
        calculo.setActivo(true);
        
        calculoRepository.save(calculo);
        log.info("✅ [CalculoGarantizadoService] Cálculo guardado - ID: {}, GarantizadoConBrandeo: {}, GarantizadoSinBrandeo: {}", 
            calculo.getId(), calculo.getGarantizadoConBrandeo(), calculo.getGarantizadoSinBrandeo());
    }
    
    /**
     * Guardar configuraciones Y PROCESAR conductores en un solo método
     * @param request Request completo con países, ciudades y cálculos
     * @return Lista de conductores procesados
     */
    @Transactional
    public GarantizadoListResponse guardarConfiguracionesYProcesar(CalcularGarantizadoRequest request) {
        log.info("🚀 [CalculoGarantizadoService] Guardando configuraciones Y procesando conductores");
        
        // Paso 1: Guardar configuraciones
        guardarConfiguraciones(request);
        
        // Paso 2: Procesar conductores de la semana anterior
        GarantizadoListResponse resultado = yegoGarantizadoRegistroService.procesarYDevolverSemanaAnterior();
        
        log.info("✅ [CalculoGarantizadoService] Configuraciones guardadas Y {} conductores procesados", 
            resultado.getConductores().size());
        
        // 🔒 REGISTRAR EL PROCESAMIENTO (BLOQUEA EL BOTÓN HASTA EL PRÓXIMO LUNES)
        procesoEstadoService.registrarProcesamiento();
        
        // 📡 ENVIAR POR WEBSOCKET PARA ACTUALIZAR LA TABLA
        systemNotificationHandler.enviarDatosCompletosGarantizado(
            resultado.getConductores(),
            resultado.getSemanaAnterior()
        );
        
        // 🎉 ENVIAR NOTIFICACIÓN DE ÉXITO
        systemNotificationHandler.sendSystemEvent("GARANTIZADO_PROCESS_SUCCESS", Map.of(
            "message", "✅ Proceso completado exitosamente. El botón estará bloqueado hasta el próximo lunes.",
            "totalConductores", resultado.getConductores().size(),
            "semana", resultado.getSemanaAnterior(),
            "autoReload", true
        ));
        
        // 🔒 ENVIAR EVENTO DE BLOQUEO DEL BOTÓN
        systemNotificationHandler.enviarEstadoProcesoGarantizado(true, "El botón está bloqueado hasta el próximo lunes");
        
        return resultado;
    }
    
    /**
     * Calcula la semana anterior del año
     */
    private String obtenerSemanaAnterior() {
        java.time.ZoneId zonaLima = java.time.ZoneId.of("America/Lima");
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now(zonaLima);
        int semanaActual = ahora.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        int semanaAnterior = semanaActual - 1;
        
        // Si estamos en la semana 1, la semana anterior es la última semana del año anterior
        if (semanaAnterior <= 0) {
            semanaAnterior = 52; // Asumimos que el año anterior tenía 52 semanas
        }
        
        return "SEMANA" + semanaAnterior;
    }
    
    /**
     * Copia automáticamente todas las configuraciones de la semana anterior a la semana actual
     * si no existen configuraciones para la semana actual
     * Este método se ejecuta automáticamente todos los lunes antes de las 8:50 AM
     */
    @Transactional
    public void copiarConfiguracionesAutomaticamente() {
        try {
            log.info("🔄 [CalculoGarantizadoService] Iniciando copia automática de configuraciones de semana anterior");
            
            // Obtener semana actual
            String semanaActual = obtenerSemanaActual();
            
            // Obtener semana anterior
            String semanaAnterior = obtenerSemanaAnterior();
            
            log.info("📅 [CalculoGarantizadoService] Semana actual: {}, Semana anterior: {}", semanaActual, semanaAnterior);
            
            // Obtener todas las combinaciones únicas de país/ciudad de la semana anterior
            List<Object[]> combinaciones = calculoRepository.findDistinctPaisAndCiudadBySemana(semanaAnterior);
            
            if (combinaciones.isEmpty()) {
                log.warn("⚠️ [CalculoGarantizadoService] No se encontraron configuraciones para la semana anterior {}", semanaAnterior);
                return;
            }
            
            log.info("📋 [CalculoGarantizadoService] Se encontraron {} combinaciones de país/ciudad en la semana anterior", combinaciones.size());
            
            int totalCopiadas = 0;
            int totalOmitidas = 0;
            
            // Iterar sobre cada combinación país/ciudad
            for (Object[] combinacion : combinaciones) {
                try {
                    String pais = (String) combinacion[0];
                    String ciudad = (String) combinacion[1];
                    
                    // Verificar si ya existen configuraciones para la semana actual
                    List<CalculoGarantizado> configuracionesExistentes = calculoRepository
                            .findAllByPaisAndCiudadAndSemana(pais.toLowerCase(), ciudad.toLowerCase(), semanaActual);
                    
                    if (!configuracionesExistentes.isEmpty()) {
                        log.debug("ℹ️ [CalculoGarantizadoService] Ya existen {} configuraciones para {} - {} - semana {}. Omitiendo.", 
                            configuracionesExistentes.size(), pais, ciudad, semanaActual);
                        totalOmitidas++;
                        continue;
                    }
                    
                    // Obtener todas las configuraciones de la semana anterior para este país/ciudad
                    List<CalculoGarantizado> configuracionesAnteriores = calculoRepository
                            .findAllByPaisAndCiudadAndSemana(pais.toLowerCase(), ciudad.toLowerCase(), semanaAnterior);
                    
                    if (configuracionesAnteriores.isEmpty()) {
                        log.warn("⚠️ [CalculoGarantizadoService] No se encontraron configuraciones para {} - {} - semana {}", 
                            pais, ciudad, semanaAnterior);
                        continue;
                    }
                    
                    // Copiar todas las configuraciones de la semana anterior a la semana actual
                    int copiadas = 0;
                    for (CalculoGarantizado configAnterior : configuracionesAnteriores) {
                        try {
                            CalculoGarantizado configNueva = new CalculoGarantizado();
                            configNueva.setPais(pais.toLowerCase());
                            configNueva.setCiudad(ciudad.toLowerCase());
                            configNueva.setSemana(semanaActual);
                            
                            // Copiar todos los valores
                            configNueva.setViajesConBrandeo(configAnterior.getViajesConBrandeo());
                            configNueva.setBonoConBrandeo(configAnterior.getBonoConBrandeo());
                            configNueva.setGarantizadoConBrandeo(configAnterior.getGarantizadoConBrandeo());
                            configNueva.setHorasConBrandeo(configAnterior.getHorasConBrandeo());
                            
                            configNueva.setViajesSinBrandeo(configAnterior.getViajesSinBrandeo());
                            configNueva.setBonoSinBrandeo(configAnterior.getBonoSinBrandeo());
                            configNueva.setGarantizadoSinBrandeo(configAnterior.getGarantizadoSinBrandeo());
                            configNueva.setHorasSinBrandeo(configAnterior.getHorasSinBrandeo());
                            
                            configNueva.setActivo(true);
                            
                            calculoRepository.save(configNueva);
                            copiadas++;
                            
                        } catch (Exception e) {
                            log.error("❌ [CalculoGarantizadoService] Error copiando una configuración individual para {} - {}: {}", 
                                pais, ciudad, e.getMessage());
                        }
                    }
                    
                    if (copiadas > 0) {
                        log.info("✅ [CalculoGarantizadoService] Se copiaron {} configuraciones para {} - {} de semana {} a semana {}", 
                            copiadas, pais, ciudad, semanaAnterior, semanaActual);
                        totalCopiadas += copiadas;
                    }
                    
                } catch (Exception e) {
                    log.error("❌ [CalculoGarantizadoService] Error procesando combinación país/ciudad: {}", e.getMessage());
                }
            }
            
            log.info("✅ [CalculoGarantizadoService] Copia automática completada: {} configuraciones copiadas, {} omitidas (ya existían)", 
                totalCopiadas, totalOmitidas);
            
        } catch (Exception e) {
            log.error("❌ [CalculoGarantizadoService] Error en copia automática de configuraciones: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene la semana actual
     */
    private String obtenerSemanaActual() {
        java.time.ZoneId zonaLima = java.time.ZoneId.of("America/Lima");
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now(zonaLima);
        int semanaActual = ahora.get(java.time.temporal.WeekFields.ISO.weekOfYear());
        return "SEMANA" + semanaActual;
    }
}

