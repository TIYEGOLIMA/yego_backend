package com.yego.backend.service;

import com.yego.backend.entity.yego_marketing_mensajes.api.request.WhatsAppMediaRequest;
import com.yego.backend.entity.yego_marketing_mensajes.api.request.WhatsAppTextRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Servicio para enviar mensajes a WhatsApp
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Service
@Slf4j
public class WhatsAppService {

    private final RestTemplate restTemplate;
    
    @Value("${whatsapp.api.key:f81bd660c7c2a537b63fc1ecda476ae6}")
    private String apiKey;
    
    @Value("${whatsapp.api.base.url:https://wsp.yego.pro}")
    private String baseUrl;
    
    private static final String TEAM = "TEAM_PERU";

    public WhatsAppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Envía un mensaje de texto a un grupo de WhatsApp
     */
    public boolean enviarTexto(String grupoId, String mensaje) {
        try {
            log.info("📤 [WhatsAppService] Enviando texto a grupo: {}", grupoId);
            
            String url = baseUrl + "/message/sendText/" + TEAM;
            
            WhatsAppTextRequest request = new WhatsAppTextRequest();
            request.setNumber(grupoId); // El número es el ID del grupo
            request.setText(mensaje);
            
            log.info("📤 [WhatsAppService] Enviando texto - number: {}, text: {}", grupoId, mensaje);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Apikey", apiKey);
            
            HttpEntity<WhatsAppTextRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [WhatsAppService] Mensaje de texto enviado exitosamente a grupo: {}", grupoId);
                log.info("📋 [WhatsAppService] Respuesta: {}", response.getBody());
                return true;
            } else {
                log.error("❌ [WhatsAppService] Error enviando texto. Status: {}", response.getStatusCode());
                log.error("❌ [WhatsAppService] Respuesta error: {}", response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [WhatsAppService] Error enviando texto a grupo {}: {}", grupoId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envía un mensaje con media a un grupo de WhatsApp
     */
    public boolean enviarMedia(String grupoId, String mediaUrl, String caption, String mediatype) {
        try {
            log.info("📤 [WhatsAppService] Enviando media a grupo: {} - Tipo: {}", grupoId, mediatype);
            
            String url = baseUrl + "/message/sendMedia/" + TEAM;
            
            // Convertir URL a base64
            String mediaBase64 = convertirUrlABase64(mediaUrl);
            if (mediaBase64 == null) {
                log.error("❌ [WhatsAppService] No se pudo convertir la URL a base64: {}", mediaUrl);
                return false;
            }
            
            WhatsAppMediaRequest request = new WhatsAppMediaRequest();
            request.setNumber(grupoId); // El número es el ID del grupo
            request.setMediatype(mediatype);
            request.setCaption(caption);
            request.setMedia(mediaBase64);
            
            log.info("📤 [WhatsAppService] Enviando media - number: {}, mediatype: {}, caption: {}, media length: {}", 
                    grupoId, mediatype, caption, mediaBase64 != null ? mediaBase64.length() : 0);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Apikey", apiKey);
            
            HttpEntity<WhatsAppMediaRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [WhatsAppService] Mensaje con media enviado exitosamente a grupo: {}", grupoId);
                log.info("📋 [WhatsAppService] Respuesta: {}", response.getBody());
                return true;
            } else {
                log.error("❌ [WhatsAppService] Error enviando media. Status: {}", response.getStatusCode());
                log.error("❌ [WhatsAppService] Respuesta error: {}", response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [WhatsAppService] Error enviando media a grupo {}: {}", grupoId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Convierte una URL de archivo a base64
     */
    private String convertirUrlABase64(String mediaUrl) {
        try {
            log.info("🔄 [WhatsAppService] Convirtiendo URL a base64: {}", mediaUrl);
            
            // Descargar el archivo desde la URL
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    mediaUrl,
                    HttpMethod.GET,
                    null,
                    byte[].class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] fileBytes = response.getBody();
                String base64 = Base64.getEncoder().encodeToString(fileBytes);
                log.info("✅ [WhatsAppService] URL convertida a base64 exitosamente ({} bytes)", fileBytes.length);
                return base64;
            } else {
                log.error("❌ [WhatsAppService] Error descargando archivo. Status: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ [WhatsAppService] Error convirtiendo URL a base64: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Detecta el tipo de media basado en la extensión del archivo
     */
    public String detectarTipoMedia(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isEmpty()) {
            return "image";
        }
        
        String urlLower = mediaUrl.toLowerCase();
        
        if (urlLower.contains(".pdf")) {
            return "pdf";
        } else if (urlLower.contains(".mp4") || urlLower.contains(".avi") || urlLower.contains(".mov") || urlLower.contains(".mkv")) {
            return "video";
        } else if (urlLower.contains(".mp3") || urlLower.contains(".wav") || urlLower.contains(".ogg") || urlLower.contains(".m4a")) {
            return "audio";
        } else {
            return "image"; // Por defecto es imagen
        }
    }

    /**
     * Envía mensajes a múltiples grupos
     * Cada grupo recibe el mensaje por separado
     */
    public void enviarAMultiplesGrupos(List<String> grupos, String mensaje, String mediaUrl) {
        if (grupos == null || grupos.isEmpty()) {
            log.warn("⚠️ [WhatsAppService] No hay grupos para enviar mensaje");
            return;
        }
        
        log.info("📤 [WhatsAppService] Enviando mensaje a {} grupos: {}", grupos.size(), grupos);
        
        int enviados = 0;
        int fallidos = 0;
        
        for (String grupoId : grupos) {
            log.info("📤 [WhatsAppService] Enviando a grupo: {}", grupoId);
            
            boolean exito = false;
            if (mediaUrl != null && !mediaUrl.isEmpty()) {
                String mediatype = detectarTipoMedia(mediaUrl);
                exito = enviarMedia(grupoId, mediaUrl, mensaje, mediatype);
            } else {
                exito = enviarTexto(grupoId, mensaje);
            }
            
            if (exito) {
                enviados++;
            } else {
                fallidos++;
            }
            
            // Pequeña pausa entre envíos para no saturar la API
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("✅ [WhatsAppService] Resumen: {} enviados, {} fallidos de {} grupos totales", 
                enviados, fallidos, grupos.size());
    }
}

