package com.yego.backend.scheduler.yego_marketing_mensajes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import com.yego.backend.repository.yego_marketing_mensajes.MarketingMensajeRepository;
import com.yego.backend.service.WhatsAppService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final WebSocketSessionService webSocketSessionService;
    
    // URL de la API de Yango
    private static final String YANGO_API_URL = "https://fleet.yango.com/api/fleet/communications/v2/mailings";
    
    // Cookie para la API de Yango (mismo pool que BaseYangoApiService). Se reemplaza park_id por el de cada flota.
    private static final String YANGO_COOKIE_TEMPLATE = "yandexuid=533847621761862983; yashr=8325443501761862983; yuidss=533847621761862983; receive-cookie-deprecation=1; gdpr=0; _ym_uid=1761862985776199037; _ym_d=1761862986; Session_id=3:1766585973.5.0.1761863223226:WbD9Jg:3d19.1.2:1|2221285626.0.2.0:3.3:1761863223|2306098505.-1.400|60:11535637.737497.XlfEa53uoqEz9AzQwGV9tlY14oY; sessar=1.1504434.CiAvoV2c7hIvDz6uawV2LJZ8MIpJV2r2HUaYHbVAHrUf4g.OihSJqGTTnGTuWPxHDcYGTMimptKIacn8a1VLJW4YnU; sessionid2=3:1766585973.5.0.1761863223226:WbD9Jg:3d19.1.2:1|2221285626.0.2.0:3.3:1761863223|2306098505.-1.400|60:11535637.737497.fakesign0000000000000000000; L=chtVZQJKQFRDdQNXVQdkBGRUTnBxcHJdFDY8OQEyAig/Ilo=.1766585973.1529459.368905.72194e695e093c8ec16117637d5f55f2; yandex_login=soporteyego; park_id=08e20910d81d42658d4334d3f6d10ac0; i=fQin2K7v2T7XHUFl7CMGLX8Jqn6b/hRN1vHca6T77f0+HL5Ugc897GlfPHo5q+F3GPUeN4jlMzCMpreqYCl3tbFRxAQ=; _ym_isad=1; yp=2081365400.multib.1#2081945973.udn.cDpzb3BvcnRleWVnbw%3D%3D#1769782060.yu.533847621761862983; ymex=1772287660.oyu.533847621761862983#2077222984.yrts.1761862984; _yasc=C/YeJEutkyg9DB6JVZD+UpHP/FPCs2Zxh735sQyQDNAPdc4vP4ch7HFpRhr6ZKONf4KV; _ym_visorc=b; bh=EkAiTm90KEE6QnJhbmQiO3Y9IjgiLCAiQ2hyb21pdW0iO3Y9IjE0NCIsICJHb29nbGUgQ2hyb21lIjt2PSIxNDQiGgN4ODYiDTE0NC4wLjc1NTkuOTcqAj8wOgcibWFjT1MiQgYxNS43LjNKAjY0UlgiTm90KEE6QnJhbmQiO3Y9IjguMC4wLjAiLCJDaHJvbWl1bSI7dj0iMTQ0LjAuNzU1OS45NyIsIkdvb2dsZSBDaHJvbWUiO3Y9IjE0NC4wLjc1NTkuOTciYMHT7csGaiHcytG2Abvxn6sE+taGzAjS0e3rA/y5r/8H3/373AfzgQI=";
    
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
                                     RestTemplate restTemplate,
                                     WebSocketSessionService webSocketSessionService) {
        this.marketingMensajeRepository = marketingMensajeRepository;
        this.whatsAppService = whatsAppService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.webSocketSessionService = webSocketSessionService;
    }

    /**
     * Se ejecuta cada 5 minutos para verificar si hay mensajes que deben enviarse
     * Optimizado: solo consulta mensajes que realmente pueden enviarse
     * Usa fixedDelay de 5 minutos (300000 ms)
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutos = 300000 ms, inicia después de 1 minuto
    public void verificarYEnviarMensajesProgramados() {
        // Verificar si hay usuarios con acceso al módulo marketing antes de ejecutar
        /*Set<String> sessionsWithAccess = webSocketSessionService.getSessionsWithModuleAccess("marketing");
        if (sessionsWithAccess.isEmpty()) {
            log.debug("⏭️ [MarketingMensajeScheduler] No hay usuarios con acceso a marketing - omitiendo verificación de mensajes programados");
            return;
        }*/
        
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
            
            // Consulta: mensajes activos con horas específicas (pueden tener grupos O flotas)
            List<MarketingMensaje> todosLosMensajesProgramados = marketingMensajeRepository
                    .findByActivoTrueAndHorasEspecificasIsNotNull();
            
            // Filtrar solo los que tienen grupos O flotas configurados
            List<MarketingMensaje> mensajesProgramados = todosLosMensajesProgramados.stream()
                    .filter(mensaje -> {
                        boolean tieneGrupos = mensaje.getGrupos() != null && !mensaje.getGrupos().trim().isEmpty();
                        boolean tieneFlotas = mensaje.getFlotas() != null && !mensaje.getFlotas().trim().isEmpty();
                        return tieneGrupos || tieneFlotas;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            if (mensajesProgramados.isEmpty()) {
                return;
            }
            
            for (MarketingMensaje mensaje : mensajesProgramados) {
                // Parsear las horas específicas
                try {
                    Map<String, List<String>> horasEspecificas = objectMapper.readValue(
                        mensaje.getHorasEspecificas(),
                        new TypeReference<Map<String, List<String>>>() {}
                    );
                    
                    for (String horaVerificar : horasAVerificar) {
                        if (horasEspecificas.containsKey(horaVerificar)) {
                            List<String> diasProgramados = horasEspecificas.get(horaVerificar);
                            
                            // Normalizar días para comparación
                            String diaActualNormalizado = normalizarDia(diaActual);
                            boolean diaCoincide = diasProgramados != null && diasProgramados.stream()
                                    .anyMatch(dia -> diaActualNormalizado.equals(normalizarDia(dia)) || 
                                                    diaActual.equals(dia) || 
                                                    normalizarDia(dia).equals(diaActual));
                            
                            if (diaCoincide) {
                                // Verificar que no se haya enviado ya para esta hora
                                if (ultimosEnvios.containsKey(mensaje.getId()) && 
                                    ultimosEnvios.get(mensaje.getId()).equals(horaVerificar)) {
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
            log.info("📤 [MarketingMensajeScheduler] Enviando mensaje ID: {} - Título: {}", mensaje.getId(), mensaje.getTitulo());
            
            // Obtener grupos
            List<String> grupos = convertirJsonALista(mensaje.getGrupos());
            boolean tieneGrupos = grupos != null && !grupos.isEmpty();
            
            // Obtener flotas
            List<String> flotas = convertirJsonALista(mensaje.getFlotas());
            boolean tieneFlotas = flotas != null && !flotas.isEmpty();
            
            if (!tieneGrupos && !tieneFlotas) {
                log.warn("⚠️ [MarketingMensajeScheduler] Mensaje ID {} no tiene grupos ni flotas configurados", mensaje.getId());
                return;
            }
            
            // Obtener nombre del archivo original si existe
            String nombreArchivoOriginal = null;
            if (mensaje.getArchivo() != null && !mensaje.getArchivo().isEmpty()) {
                nombreArchivoOriginal = extraerNombreArchivoDeUrl(mensaje.getArchivo());
            }
            
            // Obtener tipo de media
            String tipoMedia = mensaje.getTipo();
            
            // Enviar mensaje a grupos de WhatsApp si tiene grupos
            if (tieneGrupos) {
            whatsAppService.enviarAMultiplesGrupos(
                grupos,
                mensaje.getMensaje(),
                mensaje.getArchivo(),
                nombreArchivoOriginal,
                tipoMedia
            );
            }
            
            // Enviar mensaje a API de Yango (Fleet) si tiene flotas configuradas
            if (tieneFlotas) {
                enviarMensajeAYango(mensaje);
            }
            
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
     * Envía un mensaje por cada flota con su respectivo park_id
     */
    private void enviarMensajeAYango(MarketingMensaje mensaje) {
        try {
            List<String> flotas = convertirJsonALista(mensaje.getFlotas());
            if (flotas == null || flotas.isEmpty()) {
                return;
            }
            
            List<String> parkIds = obtenerTodosLosParkIds(flotas);
            if (parkIds == null || parkIds.isEmpty()) {
                return;
            }
            
            // Preparar el body según la estructura de la API de Yango
            // La API de Yango espera los campos en inglés y una estructura específica
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("type", "pro");  // Tipo de mensaje: "pro" para conductores profesionales
            requestBody.put("title", mensaje.getTitulo());
            requestBody.put("message", mensaje.getMensaje());
            
            // Estructura de recipients según el formato de la API
            Map<String, Object> recipients = new HashMap<>();
            Map<String, Object> recipientsFilters = new HashMap<>();
            recipients.put("filters", recipientsFilters);
            requestBody.put("recipients", recipients);
            
            // Enviar un mensaje por cada park_id
            int exitosos = 0;
            int fallidos = 0;
            int omitidosPorLimite = 0;
            
            for (int i = 0; i < parkIds.size(); i++) {
                String parkId = parkIds.get(i);
                try {
                    // Reemplazar park_id en la cookie plantilla por el de la flota actual
                    String cookieDinamica = YANGO_COOKIE_TEMPLATE.replaceFirst("park_id=[a-f0-9]+", "park_id=" + parkId);
                    String idempotencyToken = UUID.randomUUID().toString();
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("Cookie", cookieDinamica);
                    headers.set("x-park-id", parkId);
                    headers.set("X-Idempotency-Token", idempotencyToken);
                    
                    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                    
                    ResponseEntity<Void> response = restTemplate.exchange(
                        YANGO_API_URL,
                        HttpMethod.POST,
                        request,
                        Void.class
                    );
                    
                    int statusCodeValue = response.getStatusCode().value();
                    
                    if (statusCodeValue >= 200 && statusCodeValue < 300) {
                    exitosos++;
                        log.info("✅ [MarketingMensajeScheduler] Mensaje enviado exitosamente a Fleet - Park ID: {} | Status: {}", 
                                parkId, statusCodeValue);
                    } else {
                        fallidos++;
                        log.error("❌ [MarketingMensajeScheduler] Fallo al enviar a Fleet - Park ID: {} | Status: {}", 
                                parkId, statusCodeValue);
                    }
                    
                    if (i < parkIds.size() - 1) {
                        Thread.sleep(5000);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("❌ [MarketingMensajeScheduler] Interrupción durante el delay: {}", e.getMessage());
                    fallidos++;
                } catch (HttpClientErrorException e) {
                    int statusCodeValue = e.getStatusCode().value();
                    String responseBody = e.getResponseBodyAsString();
                    if (statusCodeValue == 400 && responseBody != null && responseBody.contains("limit_time")) {
                        omitidosPorLimite++;
                        log.warn("⚠️ [MarketingMensajeScheduler] Restricción de tiempo Fleet - Park ID: {} | La API no permite enviar otro mensaje hasta la fecha indicada (limit_time)", parkId);
                    } else if (statusCodeValue == 403) {
                        fallidos++;
                        log.warn("⚠️ [MarketingMensajeScheduler] Acceso denegado a flota - Park ID: {} | La cookie no tiene permisos para esta flota", parkId);
                    } else {
                        fallidos++;
                        log.error("❌ [MarketingMensajeScheduler] Error HTTP al enviar a Fleet - Park ID: {} | Status: {} | Error: {}", 
                                parkId, statusCodeValue, responseBody);
                    }
                } catch (HttpServerErrorException e) {
                    fallidos++;
                    int statusCodeValue = e.getStatusCode().value();
                    log.error("❌ [MarketingMensajeScheduler] Error del servidor Fleet - Park ID: {} | Status: {} | Error: {}", 
                            parkId, statusCodeValue, e.getResponseBodyAsString());
                } catch (Exception e) {
                    fallidos++;
                    log.error("❌ [MarketingMensajeScheduler] Error general al enviar a Fleet - Park ID: {} | Error: {}", 
                            parkId, e.getMessage(), e);
                    
                    if (i < parkIds.size() - 1) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            
            if (fallidos > 0 || exitosos > 0 || omitidosPorLimite > 0) {
                log.info("📊 [MarketingMensajeScheduler] Resumen Fleet - Mensaje ID: {} | ✅ Exitosos: {} | ❌ Fallidos: {} | ⏳ Omitidos (restricción tiempo): {} | Total: {}", 
                        mensaje.getId(), exitosos, fallidos, omitidosPorLimite, parkIds.size());
            }
            
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeScheduler] Error crítico enviando mensaje a Fleet - Mensaje ID: {} | Error: {}", 
                    mensaje.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Extrae todos los park_ids únicos de la lista de flotas
     * Si una flota contiene comas, separa cada parte como un park_id diferente
     * Retorna una lista con todos los park_ids únicos
     */
    private List<String> obtenerTodosLosParkIds(List<String> flotas) {
        if (flotas == null || flotas.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<String> parkIdsUnicos = new HashSet<>();
        
        for (String flota : flotas) {
            if (flota == null || flota.trim().isEmpty()) {
                continue;
            }
            
            String flotaTrim = flota.trim();
            
            // Si contiene comas, separar cada parte
            if (flotaTrim.contains(",")) {
                String[] partes = flotaTrim.split(",");
                for (String parte : partes) {
                    String parkId = parte.trim();
                    if (!parkId.isEmpty()) {
                        parkIdsUnicos.add(parkId);
                    }
                }
            } else {
                // Si no contiene comas, usar toda la flota como park_id
                parkIdsUnicos.add(flotaTrim);
            }
        }
        
        return new ArrayList<>(parkIdsUnicos);
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

