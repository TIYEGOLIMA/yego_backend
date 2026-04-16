package com.yego.backend.service;

import com.yego.backend.entity.yego_marketing_mensajes.api.request.WhatsAppMediaRequest;
import com.yego.backend.entity.yego_marketing_mensajes.api.request.WhatsAppTextRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    private static final List<String> EXTENSIONES_VIDEO = Arrays.asList(".mp4", ".avi", ".mov", ".mkv", ".webm", ".flv", ".wmv", ".3gp", ".m4v");
    private static final List<String> EXTENSIONES_DOCUMENTO = Arrays.asList(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".txt");

    public WhatsAppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Envía un mensaje de texto a un grupo de WhatsApp
     */
    public boolean enviarTexto(String grupoId, String mensaje) {
        try {
            log.info("📤 [WhatsAppService] Enviando texto a grupo: {}", grupoId);
            
            WhatsAppTextRequest request = new WhatsAppTextRequest();
            request.setNumber(grupoId);
            request.setTextMessage(new WhatsAppTextRequest.TextMessage(mensaje));
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/message/sendText/" + TEAM,
                    HttpMethod.POST,
                    crearHttpEntity(request),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [WhatsAppService] Mensaje de texto enviado exitosamente a grupo: {}", grupoId);
                return true;
            } else {
                log.error("❌ [WhatsAppService] Error enviando texto. Status: {}", response.getStatusCode());
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
    public boolean enviarMedia(String grupoId, String mediaUrl, String caption, String mediatype, String nombreArchivo) {
        try {
            log.info("📤 [WhatsAppService] Enviando media a grupo: {} - Tipo: {}", grupoId, mediatype);
            
            // Determinar el tipo de media: usar el proporcionado o detectarlo automáticamente
            if (mediatype == null || mediatype.trim().isEmpty()) {
                // Detectar automáticamente desde el nombre del archivo o la URL
                if (nombreArchivo != null && !nombreArchivo.isEmpty()) {
                    mediatype = detectarTipoMediaPorNombre(nombreArchivo);
                } else {
                    mediatype = detectarTipoMedia(mediaUrl);
                }
                log.info("🔍 [WhatsAppService] Tipo de media detectado automáticamente: {}", mediatype);
            } else {
                mediatype = normalizarMediaType(mediatype);
            }
            
            String filename = obtenerFilenameValido(nombreArchivo, mediaUrl, mediatype);
            
            WhatsAppMediaRequest request = new WhatsAppMediaRequest();
            request.setNumber(grupoId);
            request.setMediatype(mediatype);
            request.setCaption(caption != null ? caption : "");
            request.setMedia(mediaUrl); // Enviar URL directamente
            request.setFilename(filename);
            
            log.info("📋 [WhatsAppService] Enviando - mediatype: '{}', filename: '{}', url: {}", 
                    mediatype, filename, mediaUrl);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/message/sendMedia/" + TEAM,
                    HttpMethod.POST,
                    crearHttpEntity(request),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [WhatsAppService] Mensaje con media enviado exitosamente a grupo: {}", grupoId);
                return true;
            } else {
                log.error("❌ [WhatsAppService] Error enviando media. Status: {}, Response: {}", 
                        response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [WhatsAppService] Error enviando media a grupo {}: {}", grupoId, e.getMessage(), e);
            return false;
        }
    }
    
    public boolean enviarMedia(String grupoId, String mediaUrl, String caption, String mediatype) {
        return enviarMedia(grupoId, mediaUrl, caption, mediatype, null);
    }

    /**
     * Envía mensajes a múltiples grupos
     */
    public void enviarAMultiplesGrupos(List<String> grupos, String mensaje, String mediaUrl, 
                                       String nombreArchivoOriginal, String mediatypeFromRequest) {
        if (grupos == null || grupos.isEmpty()) {
            log.warn("⚠️ [WhatsAppService] No hay grupos para enviar mensaje");
            return;
        }
        
        log.info("📤 [WhatsAppService] Enviando mensaje a {} grupos", grupos.size());
        
        // Si el tipo es "ninguna" o vacío/null, solo enviar texto
        boolean enviarSoloTexto = (mediatypeFromRequest == null || 
                                   mediatypeFromRequest.trim().isEmpty() || 
                                   "ninguna".equalsIgnoreCase(mediatypeFromRequest.trim()));
        
        String mediatype = null;
        boolean debeEnviarMedia = false;
        
        if (!enviarSoloTexto && mediaUrl != null && !mediaUrl.isEmpty()) {
            mediatype = determinarTipoMedia(mediaUrl, nombreArchivoOriginal, mediatypeFromRequest);
            debeEnviarMedia = (mediatype != null && !mediatype.trim().isEmpty());
        }
        
        int enviados = 0;
        int fallidos = 0;
        
        for (String grupoId : grupos) {
            boolean exito;
            if (debeEnviarMedia) {
                log.info("📤 [WhatsAppService] Enviando media a grupo: {}", grupoId);
                exito = enviarMedia(grupoId, mediaUrl, mensaje, mediatype, nombreArchivoOriginal);
            } else {
                log.info("📤 [WhatsAppService] Enviando solo texto a grupo: {}", grupoId);
                exito = enviarTexto(grupoId, mensaje);
            }
            
            if (exito) enviados++;
            else fallidos++;
            
            // Pausa entre envíos
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        log.info("✅ [WhatsAppService] Resumen: {} enviados, {} fallidos de {} grupos", 
                enviados, fallidos, grupos.size());
    }
    
    public void enviarAMultiplesGrupos(List<String> grupos, String mensaje, String mediaUrl) {
        enviarAMultiplesGrupos(grupos, mensaje, mediaUrl, null, null);
    }
    
    public void enviarAMultiplesGrupos(List<String> grupos, String mensaje, String mediaUrl, String nombreArchivoOriginal) {
        enviarAMultiplesGrupos(grupos, mensaje, mediaUrl, nombreArchivoOriginal, null);
    }

    /**
     * Obtiene un filename válido, extrayéndolo de la URL o generando uno por defecto
     */
    private String obtenerFilenameValido(String nombreArchivo, String mediaUrl, String mediatype) {
        String filename = nombreArchivo;
        
        // Intentar extraer de la URL si no se proporcionó
        if (filename == null || filename.trim().isEmpty()) {
            filename = extraerNombreArchivoDeUrl(mediaUrl);
        }
        
        // Limpiar espacios
        if (filename != null) {
            filename = filename.trim();
        }
        
        // Validar extensión para documentos
        if ("document".equals(mediatype) && filename != null && !filename.isEmpty()) {
            String filenameLower = filename.toLowerCase();
            boolean tieneExtensionValida = EXTENSIONES_DOCUMENTO.stream()
                    .anyMatch(filenameLower::endsWith);
            if (!tieneExtensionValida && !filename.contains(".")) {
                filename = filename + ".pdf";
            }
        }
        
        // Generar nombre por defecto si aún no hay uno válido
        if (filename == null || filename.isEmpty()) {
            String extension = obtenerExtensionPorTipo(mediatype);
            filename = "archivo" + extension;
            log.warn("⚠️ [WhatsAppService] Usando nombre por defecto: {}", filename);
        }
        
        return filename;
    }

    /**
     * Extrae el nombre del archivo de una URL y lo decodifica
     */
    private String extraerNombreArchivoDeUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isEmpty()) {
            return null;
        }
        
        try {
            int lastSlash = mediaUrl.lastIndexOf('/');
            if (lastSlash < 0 || lastSlash >= mediaUrl.length() - 1) {
                return null;
            }
            
            String parte = mediaUrl.substring(lastSlash + 1);
            int queryIndex = parte.indexOf('?');
            if (queryIndex > 0) parte = parte.substring(0, queryIndex);
            
            int hashIndex = parte.indexOf('#');
            if (hashIndex > 0) parte = parte.substring(0, hashIndex);
            
            if (!parte.isEmpty()) {
                return URLDecoder.decode(parte, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("⚠️ [WhatsAppService] Error extrayendo nombre de archivo: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Determina el tipo de media usando prioridad: request > nombre archivo > URL
     */
    private String determinarTipoMedia(String mediaUrl, String nombreArchivo, String mediatypeFromRequest) {
        if (mediaUrl == null || mediaUrl.isEmpty()) {
            return null;
        }
        
        // Prioridad 1: Tipo del request
        if (mediatypeFromRequest != null && !mediatypeFromRequest.trim().isEmpty()) {
            return normalizarMediaType(mediatypeFromRequest);
        }
        
        // Prioridad 2: Detectar por nombre del archivo
        if (nombreArchivo != null && !nombreArchivo.isEmpty()) {
            return detectarTipoMediaPorNombre(nombreArchivo);
        }
        
        // Prioridad 3: Detectar por URL
        return detectarTipoMedia(mediaUrl);
    }

    /**
     * Detecta el tipo de media basado en la extensión del archivo
     */
    public String detectarTipoMedia(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isEmpty()) {
            return "image";
        }
        
        String urlLower = mediaUrl.toLowerCase();
        String extension = extraerExtension(urlLower);
        
        if (extension != null && extension.equals(".pdf") || urlLower.contains(".pdf")) {
            return "document";
        }
        
        if (extension != null && EXTENSIONES_VIDEO.contains(extension) || 
            EXTENSIONES_VIDEO.stream().anyMatch(urlLower::contains)) {
            return "video";
        }
        
        return "image";
    }

    /**
     * Detecta el tipo de media basado en el nombre del archivo
     */
    public String detectarTipoMediaPorNombre(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isEmpty()) {
            return "image";
        }
        
        String nombreLower = nombreArchivo.toLowerCase();
        
        if (nombreLower.endsWith(".pdf")) {
            return "document";
        }
        
        if (EXTENSIONES_VIDEO.stream().anyMatch(nombreLower::endsWith)) {
            return "video";
        }
        
        return "image";
    }

    /**
     * Extrae la extensión de una URL
     */
    private String extraerExtension(String url) {
        int lastDot = url.lastIndexOf('.');
        if (lastDot <= 0) return null;
        
        int queryStart = url.indexOf('?', lastDot);
        int hashStart = url.indexOf('#', lastDot);
        int endIndex = url.length();
        
        if (queryStart > 0 && queryStart < endIndex) endIndex = queryStart;
        if (hashStart > 0 && hashStart < endIndex) endIndex = hashStart;
        
        return url.substring(lastDot, endIndex);
    }

    /**
     * Normaliza el tipo de media a los valores aceptados por la API
     */
    private String normalizarMediaType(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return "image";
        }
        
        String tipoLower = tipo.toLowerCase().trim();
        
        switch (tipoLower) {
            case "image":
            case "imagen":
                return "image";
            case "document":
            case "documento":
            case "pdf":
                return "document";
            case "video":
                return "video";
            default:
                log.warn("⚠️ [WhatsAppService] Tipo desconocido: {}, usando 'image'", tipo);
                return "image";
        }
    }

    /**
     * Obtiene la extensión de archivo basada en el tipo de media
     */
    private String obtenerExtensionPorTipo(String mediatype) {
        switch (mediatype != null ? mediatype.toLowerCase() : "") {
            case "document":
                return ".pdf";
            case "video":
                return ".mp4";
            default:
                return ".jpg";
        }
    }

    /**
     * Convierte una URL de archivo a base64
     * NOTA: No se usa actualmente - se envía la URL directamente
     */
    @SuppressWarnings("unused")
    private String convertirUrlABase64(String mediaUrl) {
        try {
            log.info("🔄 [WhatsAppService] Convirtiendo URL a base64: {}", mediaUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
            headers.set("User-Agent", "Mozilla/5.0");
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    mediaUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] fileBytes = response.getBody();
                if (fileBytes.length == 0) {
                    log.error("❌ [WhatsAppService] Archivo descargado está vacío");
                    return null;
                }
                
                String base64 = Base64.getEncoder().encodeToString(fileBytes);
                log.info("✅ [WhatsAppService] Convertido a base64 - Archivo: {} bytes, Base64: {} caracteres", 
                        fileBytes.length, base64.length());
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
     * Crea un HttpEntity con headers comunes
     */
    private <T> HttpEntity<T> crearHttpEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Apikey", apiKey);
        return new HttpEntity<>(body, headers);
    }
}
