package com.yego.backend.config.yego_marketing_mensajes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import com.yego.backend.repository.yego_marketing_mensajes.MarketingMensajeRepository;
import com.yego.backend.service.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Scheduler para enviar mensajes de marketing a las horas programadas
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Component
@Slf4j
public class MarketingMensajeScheduler {

    private final MarketingMensajeRepository marketingMensajeRepository;
    private final WhatsAppService whatsAppService;
    private final ObjectMapper objectMapper;
    
    // Mapa para evitar envíos duplicados en el mismo minuto
    private final Map<Long, String> ultimosEnvios = new HashMap<>();
    
    // Formato de hora: HH:mm
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    // Mapeo de días de la semana en inglés a español abreviado
    private static final Map<String, String> DIAS_SEMANA = Map.of(
        "MONDAY", "Lun",
        "TUESDAY", "Mar",
        "WEDNESDAY", "Mié",
        "THURSDAY", "Jue",
        "FRIDAY", "Vie",
        "SATURDAY", "Sáb",
        "SUNDAY", "Dom"
    );
    
    // Mapeo alternativo para variaciones comunes
    private static final Map<String, String> DIAS_ALTERNATIVOS = Map.of(
        "Mie", "Mié",
        "Sab", "Sáb"
    );

    public MarketingMensajeScheduler(MarketingMensajeRepository marketingMensajeRepository,
                                     WhatsAppService whatsAppService,
                                     ObjectMapper objectMapper) {
        this.marketingMensajeRepository = marketingMensajeRepository;
        this.whatsAppService = whatsAppService;
        this.objectMapper = objectMapper;
    }

    /**
     * Se ejecuta cada 5 minutos para verificar si hay mensajes que deben enviarse
     * Optimizado: solo consulta mensajes que realmente pueden enviarse
     * Usa fixedDelay de 5 minutos (300000 ms)
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutos = 300000 ms, inicia después de 1 minuto
    public void verificarYEnviarMensajesProgramados() {
        log.info("⏰ [MarketingMensajeScheduler] Scheduler ejecutándose - Verificando mensajes programados");
        try {
            LocalTime horaActual = LocalTime.now(java.time.ZoneId.of("America/Lima"));
            String diaActual = obtenerDiaActual();
            
            // Generar lista de horas a verificar: hora actual y las últimas 4 horas (últimos 5 minutos)
            List<String> horasAVerificar = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                LocalTime horaVerificar = horaActual.minusMinutes(i);
                horasAVerificar.add(horaVerificar.format(HORA_FORMATTER));
            }
            
            log.info("🕐 [MarketingMensajeScheduler] Verificando horas: {} - Día: {}", horasAVerificar, diaActual);
            
            // Consulta optimizada: solo mensajes activos con WhatsApp activado y horas específicas
            List<MarketingMensaje> mensajesProgramados = marketingMensajeRepository
                    .findByActivoTrueAndWhatsappTrueAndHorasEspecificasIsNotNull();
            
            if (mensajesProgramados.isEmpty()) {
                log.info("ℹ️ [MarketingMensajeScheduler] No hay mensajes programados para verificar");
                return;
            }
            
            log.info("📋 [MarketingMensajeScheduler] Verificando {} mensajes programados", mensajesProgramados.size());
            
            for (MarketingMensaje mensaje : mensajesProgramados) {
                // Verificar que tenga grupos configurados
                if (mensaje.getGrupos() == null || mensaje.getGrupos().trim().isEmpty()) {
                    continue;
                }
                
                // Parsear las horas específicas
                try {
                    Map<String, List<String>> horasEspecificas = objectMapper.readValue(
                        mensaje.getHorasEspecificas(),
                        new TypeReference<Map<String, List<String>>>() {}
                    );
                    
                    // Verificar cada hora del rango (últimos 5 minutos)
                    for (String horaVerificar : horasAVerificar) {
                        if (horasEspecificas.containsKey(horaVerificar)) {
                            List<String> diasProgramados = horasEspecificas.get(horaVerificar);
                            
                            // Normalizar días para comparación
                            String diaActualNormalizado = normalizarDia(diaActual);
                            boolean diaCoincide = diasProgramados != null && diasProgramados.stream()
                                    .anyMatch(dia -> diaActualNormalizado.equals(normalizarDia(dia)) || 
                                                    diaActual.equals(dia) || 
                                                    normalizarDia(dia).equals(diaActual));
                            
                            // Verificar si el día actual está en la lista de días programados
                            if (diaCoincide) {
                                // Verificar que no se haya enviado ya para esta hora
                                if (ultimosEnvios.containsKey(mensaje.getId()) && 
                                    ultimosEnvios.get(mensaje.getId()).equals(horaVerificar)) {
                                    log.debug("⏭️ [MarketingMensajeScheduler] Mensaje {} ya enviado para hora {}", 
                                            mensaje.getId(), horaVerificar);
                                    continue;
                                }
                                
                                // Enviar el mensaje
                                enviarMensajeProgramado(mensaje);
                                
                                // Registrar el envío para evitar duplicados
                                ultimosEnvios.put(mensaje.getId(), horaVerificar);
                                
                                // Limpiar envíos antiguos
                                limpiarEnviosAntiguos();
                                
                                // Solo enviar una vez por mensaje, aunque haya múltiples horas en el rango
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ [MarketingMensajeScheduler] Error parseando horas específicas del mensaje {}: {}", 
                            mensaje.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeScheduler] Error en verificación de mensajes programados: {}", 
                    e.getMessage(), e);
        }
    }

    /**
     * Envía un mensaje programado
     */
    private void enviarMensajeProgramado(MarketingMensaje mensaje) {
        try {
            log.info("📤 [MarketingMensajeScheduler] Enviando mensaje programado ID: {} - Título: {}", 
                    mensaje.getId(), mensaje.getTitulo());
            
            // Obtener grupos
            List<String> grupos = convertirJsonALista(mensaje.getGrupos());
            if (grupos == null || grupos.isEmpty()) {
                log.warn("⚠️ [MarketingMensajeScheduler] No hay grupos para enviar mensaje ID: {}", mensaje.getId());
                return;
            }
            
            // Obtener nombre del archivo original si existe
            String nombreArchivoOriginal = null;
            if (mensaje.getArchivo() != null && !mensaje.getArchivo().isEmpty()) {
                // Extraer nombre del archivo de la URL
                nombreArchivoOriginal = extraerNombreArchivoDeUrl(mensaje.getArchivo());
            }
            
            // Obtener tipo de media
            String tipoMedia = mensaje.getTipo();
            
            // Enviar mensaje
            whatsAppService.enviarAMultiplesGrupos(
                grupos,
                mensaje.getMensaje(),
                mensaje.getArchivo(),
                nombreArchivoOriginal,
                tipoMedia
            );
            
            log.info("✅ [MarketingMensajeScheduler] Mensaje programado enviado exitosamente ID: {}", mensaje.getId());
            
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeScheduler] Error enviando mensaje programado ID {}: {}", 
                    mensaje.getId(), e.getMessage(), e);
        }
    }

    /**
     * Obtiene el día actual en formato abreviado (Lun, Mar, Mié, etc.)
     */
    private String obtenerDiaActual() {
        java.time.DayOfWeek diaSemana = java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).getDayOfWeek();
        String diaIngles = diaSemana.toString();
        
        // Convertir de inglés a español
        return DIAS_SEMANA.getOrDefault(diaIngles, diaIngles);
    }
    
    /**
     * Normaliza el día para comparación (acepta variaciones como "Mie"/"Mié", "Sab"/"Sáb")
     */
    private String normalizarDia(String dia) {
        if (dia == null) return null;
        return DIAS_ALTERNATIVOS.getOrDefault(dia, dia);
    }

    /**
     * Convierte JSON string a List<String>
     */
    private List<String> convertirJsonALista(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("⚠️ [MarketingMensajeScheduler] Error parseando JSON a lista: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Extrae el nombre del archivo de una URL
     */
    private String extraerNombreArchivoDeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                String fileName = url.substring(lastSlash + 1);
                int queryIndex = fileName.indexOf('?');
                if (queryIndex > 0) {
                    fileName = fileName.substring(0, queryIndex);
                }
                return fileName;
            }
        } catch (Exception e) {
            log.warn("⚠️ [MarketingMensajeScheduler] Error extrayendo nombre de archivo: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Limpia los registros de envíos antiguos para liberar memoria
     */
    private void limpiarEnviosAntiguos() {
        // Limpiar cada 100 envíos para no hacerlo en cada iteración
        if (ultimosEnvios.size() > 100) {
            ultimosEnvios.clear();
            log.debug("🧹 [MarketingMensajeScheduler] Limpiados registros de envíos antiguos");
        }
    }
}

