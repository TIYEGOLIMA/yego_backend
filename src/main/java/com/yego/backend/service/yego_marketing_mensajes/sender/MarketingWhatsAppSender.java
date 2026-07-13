package com.yego.backend.service.yego_marketing_mensajes.sender;

import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class MarketingWhatsAppSender {

    private static final List<String> VIDEO_EXTENSIONS =
            List.of(".mp4", ".avi", ".mov", ".mkv", ".webm", ".flv", ".wmv", ".3gp", ".m4v");
    private static final List<String> DOCUMENT_EXTENSIONS =
            List.of(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".txt");

    private final RestTemplate restTemplate;
    private final MarketingDispatchTracker dispatchTracker;
    private final String baseUrl;
    private final String token;
    private final String instance;
    private final String groupsPath;
    private final long delayMs;

    public MarketingWhatsAppSender(
            @Qualifier("whatsAppRestTemplate") RestTemplate restTemplate,
            MarketingDispatchTracker dispatchTracker,
            @Value("${evolution-go.marketing.base-url}") String baseUrl,
            @Value("${evolution-go.marketing.token:}") String token,
            @Value("${evolution-go.marketing.instance}") String instance,
            @Value("${evolution-go.marketing.groups-path}") String groupsPath,
            @Value("${evolution-go.marketing.delay-ms}") long delayMs) {
        this.restTemplate = restTemplate;
        this.dispatchTracker = dispatchTracker;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.token = token == null ? "" : token.trim();
        this.instance = instance == null ? "" : instance.trim();
        this.groupsPath = normalizePath(groupsPath);
        this.delayMs = Math.max(0, delayMs);
    }

    public MarketingDeliveryResult enviar(
            MarketingMensaje mensaje, List<String> grupos, Instant scheduledFor) {
        List<String> destinos = grupos == null ? Collections.emptyList() : grupos.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (destinos.isEmpty()) {
            return MarketingDeliveryResult.vacio();
        }
        boolean conMedia = mensaje.getArchivo() != null
                && !mensaje.getArchivo().isBlank()
                && mensaje.getTipo() != null
                && !mensaje.getTipo().isBlank()
                && !"ninguna".equalsIgnoreCase(mensaje.getTipo().trim());
        int enviados = 0;
        int fallidos = 0;
        int omitidos = 0;

        for (int i = 0; i < destinos.size(); i++) {
            String destino = destinos.get(i);
            Optional<MarketingDispatchTracker.Claim> claim = dispatchTracker.reclamar(
                    mensaje.getId(),
                    MarketingDispatchTracker.CHANNEL_WHATSAPP,
                    destino,
                    scheduledFor);
            if (claim.isEmpty()) {
                omitidos++;
                continue;
            }

            if (token.isBlank()) {
                dispatchTracker.marcarFallido(
                        claim.get(), null, "EVOLUTION_GO_MARKETING_TOKEN no configurado");
                fallidos++;
                continue;
            }

            SendOutcome outcome = conMedia
                    ? enviarMedia(destino, mensaje)
                    : enviarTexto(destino, mensaje.getMensaje());
            if (outcome.success()) {
                dispatchTracker.marcarEnviado(
                        claim.get(), outcome.httpStatus(), outcome.providerMessageId());
                enviados++;
            } else {
                dispatchTracker.marcarFallido(
                        claim.get(), outcome.httpStatus(), outcome.errorMessage());
                fallidos++;
            }
            pausar(i, destinos.size());
        }

        MarketingDeliveryResult result = new MarketingDeliveryResult(
                enviados, fallidos, omitidos, destinos.size());
        log.info("[MarketingWhatsApp] Resumen instance={} enviados={} fallidos={} omitidos={} total={}",
                instance, result.enviados(), result.fallidos(), result.omitidos(), result.total());
        return result;
    }

    public List<Map<String, Object>> obtenerGrupos() {
        if (token.isBlank()) {
            log.error("[MarketingWhatsApp] No se pueden sincronizar grupos: token no configurado");
            return Collections.emptyList();
        }
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + groupsPath,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Collections.emptyList();
            }
            Object data = response.getBody().get("data");
            if (!(data instanceof List<?> groups)) {
                log.error("[MarketingWhatsApp] Respuesta de grupos sin arreglo data");
                return Collections.emptyList();
            }
            List<Map<String, Object>> result = groups.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(this::normalizarGrupo)
                    .filter(group -> group.get("id") != null)
                    .toList();
            log.info("[MarketingWhatsApp] Grupos sincronizados instance={} total={}",
                    instance, result.size());
            return result;
        } catch (RestClientResponseException e) {
            log.error("[MarketingWhatsApp] Error sincronizando grupos. status={}", e.getRawStatusCode());
        } catch (Exception e) {
            log.error("[MarketingWhatsApp] Error sincronizando grupos: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private Map<String, Object> normalizarGrupo(Map<?, ?> source) {
        Map<String, Object> group = new HashMap<>();
        group.put("id", source.get("JID"));
        group.put("subject", source.get("Name"));
        group.put("subjectOwner", source.get("NameSetBy"));
        group.put("subjectTime", parseEpochSeconds(source.get("NameSetAt")));
        group.put("size", source.get("ParticipantCount"));
        group.put("creation", parseEpochSeconds(source.get("GroupCreated")));
        group.put("owner", source.get("OwnerJID"));
        group.put("desc", source.get("Topic"));
        group.put("descId", source.get("TopicID"));
        group.put("restrict", source.get("IsLocked"));
        group.put("announce", source.get("IsAnnounce"));
        group.put("isCommunity", source.get("IsParent"));
        group.put("isCommunityAnnounce", source.get("IsDefaultSubGroup"));
        return group;
    }

    private Long parseEpochSeconds(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.toString()).getEpochSecond();
        } catch (Exception ignored) {
            return null;
        }
    }

    private SendOutcome enviarTexto(String destino, String texto) {
        Map<String, Object> request = Map.of(
                "number", destino,
                "text", sanitizarTexto(texto),
                "delay", delayMs);
        return post("/send/text", request, destino, "texto");
    }

    private SendOutcome enviarMedia(String destino, MarketingMensaje mensaje) {
        String mediaType = resolveMediaType(
                mensaje.getTipo(), mensaje.getArchivo());
        Map<String, Object> request = Map.of(
                "number", destino,
                "mediatype", mediaType,
                "caption", sanitizarTexto(mensaje.getMensaje()),
                "media", mensaje.getArchivo(),
                "fileName", resolveFilename(mensaje.getArchivo(), mediaType),
                "delay", delayMs);
        return post("/send/media", request, destino, mediaType);
    }

    private SendOutcome post(String path, Object request, String destino, String tipo) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + path,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers()),
                    String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[MarketingWhatsApp] Enviado tipo={} destino={}", tipo, destino);
                return SendOutcome.success(response.getStatusCode().value());
            }
            log.error("[MarketingWhatsApp] Fallo tipo={} destino={} status={}",
                    tipo, destino, response.getStatusCode().value());
            return SendOutcome.failure(
                    response.getStatusCode().value(), "Respuesta no exitosa de Evolution Go");
        } catch (RestClientResponseException e) {
            log.error("[MarketingWhatsApp] Fallo tipo={} destino={} status={}",
                    tipo, destino, e.getRawStatusCode());
            return SendOutcome.failure(e.getRawStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[MarketingWhatsApp] Error tipo={} destino={}: {}",
                    tipo, destino, e.getMessage());
            return SendOutcome.failure(null, e.getMessage());
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("apikey", token);
        headers.setBearerAuth(token);
        return headers;
    }

    private String resolveMediaType(String requestedType, String mediaUrl) {
        String type = requestedType == null ? "" : requestedType.trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "document", "documento", "pdf" -> "document";
            case "video" -> "video";
            case "image", "imagen" -> "image";
            default -> detectMediaType(mediaUrl);
        };
    }

    private String detectMediaType(String mediaUrl) {
        String value = mediaUrl == null ? "" : mediaUrl.toLowerCase(Locale.ROOT);
        if (DOCUMENT_EXTENSIONS.stream().anyMatch(value::contains)) {
            return "document";
        }
        if (VIDEO_EXTENSIONS.stream().anyMatch(value::contains)) {
            return "video";
        }
        return "image";
    }

    private String resolveFilename(String mediaUrl, String mediaType) {
        if (mediaUrl != null && !mediaUrl.isBlank()) {
            try {
                String path = mediaUrl.substring(mediaUrl.lastIndexOf('/') + 1);
                int query = path.indexOf('?');
                if (query >= 0) {
                    path = path.substring(0, query);
                }
                if (!path.isBlank()) {
                    return URLDecoder.decode(path, StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                log.debug("[MarketingWhatsApp] No se pudo extraer filename: {}", e.getMessage());
            }
        }
        return switch (mediaType) {
            case "document" -> "archivo.pdf";
            case "video" -> "archivo.mp4";
            default -> "archivo.jpg";
        };
    }

    private void pausar(int index, int total) {
        if (delayMs <= 0 || index >= total - 1) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String sanitizarTexto(String texto) {
        return texto == null ? "" : texto.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("evolution-go.marketing.base-url es obligatorio");
        }
        return value.trim().replaceAll("/+$", "");
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("evolution-go.marketing.groups-path es obligatorio");
        }
        String clean = value.trim();
        return clean.startsWith("/") ? clean : "/" + clean;
    }

    private record SendOutcome(
            boolean success,
            Integer httpStatus,
            String providerMessageId,
            String errorMessage) {

        private static SendOutcome success(Integer httpStatus) {
            return new SendOutcome(true, httpStatus, null, null);
        }

        private static SendOutcome failure(Integer httpStatus, String errorMessage) {
            return new SendOutcome(false, httpStatus, null, errorMessage);
        }
    }
}
