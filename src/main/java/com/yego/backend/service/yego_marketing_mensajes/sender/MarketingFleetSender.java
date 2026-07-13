package com.yego.backend.service.yego_marketing_mensajes.sender;

import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

@Service
@Slf4j
public class MarketingFleetSender {

    private final RestTemplate restTemplate;
    private final MarketingDispatchTracker dispatchTracker;
    private final String apiUrl;
    private final String cookieTemplate;
    private final long delayMs;

    public MarketingFleetSender(
            RestTemplate restTemplate,
            MarketingDispatchTracker dispatchTracker,
            @Value("${marketing.fleet.api-url}") String apiUrl,
            @Value("${marketing.fleet.cookie:}") String cookieTemplate,
            @Value("${marketing.fleet.delay-ms}") long delayMs) {
        this.restTemplate = restTemplate;
        this.dispatchTracker = dispatchTracker;
        this.apiUrl = apiUrl;
        this.cookieTemplate = cookieTemplate == null ? "" : cookieTemplate.trim();
        this.delayMs = Math.max(0, delayMs);
    }

    public MarketingDeliveryResult enviar(
            MarketingMensaje mensaje, List<String> flotas, Instant scheduledFor) {
        List<String> parkIds = normalizeParkIds(flotas);
        if (parkIds.isEmpty()) {
            return MarketingDeliveryResult.vacio();
        }
        Map<String, Object> body = Map.of(
                "type", "pro",
                "title", mensaje.getTitulo(),
                "message", mensaje.getMensaje(),
                "recipients", Map.of("filters", Map.of()));

        int enviados = 0;
        int fallidos = 0;
        int omitidos = 0;

        for (int i = 0; i < parkIds.size(); i++) {
            String parkId = parkIds.get(i);
            Optional<MarketingDispatchTracker.Claim> claim = dispatchTracker.reclamar(
                    mensaje.getId(),
                    MarketingDispatchTracker.CHANNEL_FLEET,
                    parkId,
                    scheduledFor);
            if (claim.isEmpty()) {
                omitidos++;
                continue;
            }

            if (cookieTemplate.isBlank()) {
                dispatchTracker.marcarFallido(
                        claim.get(), null, "YEGO_MARKETING_FLEET_COOKIE no configurada");
                fallidos++;
                continue;
            }

            try {
                ResponseEntity<Void> response = restTemplate.exchange(
                        apiUrl,
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers(parkId, claim.get().idempotencyKey())),
                        Void.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    enviados++;
                    dispatchTracker.marcarEnviado(
                            claim.get(), response.getStatusCode().value(), null);
                    log.info("[MarketingFleet] Enviado parkId={}", parkId);
                } else {
                    fallidos++;
                    dispatchTracker.marcarFallido(
                            claim.get(),
                            response.getStatusCode().value(),
                            "Respuesta no exitosa de Fleet");
                    log.error("[MarketingFleet] Fallo parkId={} status={}",
                            parkId, response.getStatusCode().value());
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 400
                        && e.getResponseBodyAsString().contains("limit_time")) {
                    omitidos++;
                    dispatchTracker.marcarOmitido(
                            claim.get(), e.getStatusCode().value(), "limit_time");
                    log.warn("[MarketingFleet] Omitido por limit_time parkId={}", parkId);
                } else {
                    fallidos++;
                    dispatchTracker.marcarFallido(
                            claim.get(), e.getStatusCode().value(), e.getResponseBodyAsString());
                    log.error("[MarketingFleet] Fallo parkId={} status={}",
                            parkId, e.getStatusCode().value());
                }
            } catch (RestClientResponseException e) {
                fallidos++;
                dispatchTracker.marcarFallido(
                        claim.get(), e.getRawStatusCode(), e.getResponseBodyAsString());
                log.error("[MarketingFleet] Fallo parkId={} status={}",
                        parkId, e.getRawStatusCode());
            } catch (Exception e) {
                fallidos++;
                dispatchTracker.marcarFallido(claim.get(), null, e.getMessage());
                log.error("[MarketingFleet] Error parkId={}: {}", parkId, e.getMessage());
            }
            pausar(i, parkIds.size());
        }

        MarketingDeliveryResult result = new MarketingDeliveryResult(
                enviados, fallidos, omitidos, parkIds.size());
        log.info("[MarketingFleet] Resumen enviados={} fallidos={} omitidos={} total={}",
                result.enviados(), result.fallidos(), result.omitidos(), result.total());
        return result;
    }

    private HttpHeaders headers(String parkId, UUID idempotencyKey) {
        String cookie = cookieTemplate.contains("park_id=")
                ? cookieTemplate.replaceFirst("park_id=[^;]*", "park_id=" + parkId)
                : cookieTemplate + "; park_id=" + parkId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", cookie);
        headers.set("x-park-id", parkId);
        headers.set("X-Idempotency-Token", idempotencyKey.toString());
        return headers;
    }

    private List<String> normalizeParkIds(List<String> flotas) {
        if (flotas == null) {
            return List.of();
        }
        return flotas.stream()
                .filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
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
}
