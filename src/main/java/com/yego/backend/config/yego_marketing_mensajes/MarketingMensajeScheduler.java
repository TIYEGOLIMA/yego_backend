package com.yego.backend.config.yego_marketing_mensajes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import com.yego.backend.repository.yego_marketing_mensajes.MarketingMensajeRepository;
import com.yego.backend.service.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;
    
    // URL de la API de Yango
    private static final String YANGO_API_URL = "https://fleet.yango.com/api/fleet/communications/v2/mailings";
    
    // Headers para la API de Yango
    private static final String YANGO_COOKIE = "i=PSsBLgVrJpYnJ2+C+lS7y26viiqCUpy2WbUIgyUcFtABmXdLBimLbHRDxBwcqG4Ld1g9EZ9dSEYH/4lPLPP4oO1vzOA=; yandexuid=6319448971755539422; yashr=4577633351755539422; gdpr=0; _ym_uid=175269441917135422; _ym_d=1755539425; yp=2070899442.udn.cDpnaW9tYXJvcnRlZ2E%3D; L=dyJbeVNbRwUDZF5pWQB1c1RfQ3hdYXJ6MTsmVVQ2XQAyDSIU.1755539442.16252.380690.a38c48704c9060f72c19a8a74895912e; yandex_login=giomarortega; Session_id=3:1756824708.5.0.1755539442239:WbD9Jg:8b7b.1.2:1|2223153146.0.2.0:3.3:1755539442|60:11147555.299236.OWQHwoNzzJ1nBLc3-rcfhkPu4EM; sessar=1.1205.CiDhzz0LO9Eyn6IfEuzHmK4ql5cl2LAKy2S4u3lUMntEwA.13627qed3lFkVeQ7A2EN777bRR2QYAzWq6Hmt4Fbt2w; sessionid2=3:1756824708.5.0.1755539442239:WbD9Jg:8b7b.1.2:1|2223153146.0.2.0:3.3:1755539442|60:11147555.299236.fakesign0000000000000000000; park_id=08e20910d81d42658d4334d3f6d10ac0; yuidss=6319448971755539422; ymex=2076954182.yrts.1761594182; _ym_isad=2; _ym_visorc=b; _yasc=OmQBtTw8vSnInrmz+Igq9+EwmJy3O0OHgVaDjxzeX/dBcpP1EKPY8eWqSw/D2GQezo+f; bh=EkIiQ2hyb21pdW0iO3Y9IjE0MiIsICJNaWNyb3NvZnQgRWRnZSI7dj0iMTQyIiwgIk5vdF9BIEJyYW5kIjt2PSI5OSIaA3g4NiINMTQyLjAuMzU5NS45NCoCPzA6CSJXaW5kb3dzIkIGMTkuMC4wSgI2NFJbIkNocm9taXVtIjt2PSIxNDIuMC43NDQ0LjE3NiIsIk1pY3Jvc29mdCBFZGdlIjt2PSIxNDIuMC4zNTk1Ljk0IiwiTm90X0EgQnJhbmQiO3Y9Ijk5LjAuMC4wImDYg5jJBmoh3Mrh/wiS2KGxA5/P4eoD+/rw5w3r//32D/iczIcI2egC";
    private static final String YANGO_PARK_ID = "08e20910d81d42658d4334d3f6d10ac0";
    
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
                                     ObjectMapper objectMapper,
                                     RestTemplate restTemplate) {
        this.marketingMensajeRepository = marketingMensajeRepository;
        this.whatsAppService = whatsAppService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
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
            
            // Enviar mensaje a grupos de WhatsApp
            whatsAppService.enviarAMultiplesGrupos(
                grupos,
                mensaje.getMensaje(),
                mensaje.getArchivo(),
                nombreArchivoOriginal,
                tipoMedia
            );
            
            // Enviar mensaje a API de Yango solo si tiene flotas configuradas
            List<String> flotas = convertirJsonALista(mensaje.getFlotas());
            if (flotas != null && !flotas.isEmpty()) {
                enviarMensajeAYango(mensaje);
            } else {
                log.debug("⏭️ [MarketingMensajeScheduler] Mensaje ID {} no tiene flotas configuradas, no se enviará a API Yango", mensaje.getId());
            }
            
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
     * Envía el mensaje a la API de Yango
     * Solo envía título y mensaje
     * Solo se ejecuta si el mensaje tiene flotas configuradas
     */
    private void enviarMensajeAYango(MarketingMensaje mensaje) {
        try {
            log.info("📤 [MarketingMensajeScheduler] Enviando mensaje a API Yango - Título: {}", mensaje.getTitulo());
            
            // Preparar el body con solo título y mensaje
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("titulo", mensaje.getTitulo());
            requestBody.put("mensaje", mensaje.getMensaje());
            
            // Configurar headers con Cookie y x-park-id
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cookie", YANGO_COOKIE);
            headers.set("x-park-id", YANGO_PARK_ID);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            // Enviar POST a la API de Yango
            restTemplate.exchange(
                YANGO_API_URL,
                HttpMethod.POST,
                request,
                Void.class
            );
            
            log.info("✅ [MarketingMensajeScheduler] Mensaje enviado exitosamente a API Yango - Título: {}", mensaje.getTitulo());
            
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeScheduler] Error enviando mensaje a API Yango ID {}: {}", 
                    mensaje.getId(), e.getMessage(), e);
        }
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

