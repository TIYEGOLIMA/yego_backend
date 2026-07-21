package com.yego.backend.service.yego_api_externo;

import com.yego.backend.config.yego_pro_ops.YegoProOpsProperties;
import com.yego.backend.integration.YangoCookiePool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP Yango para yego_api_externo: pool de cookies y retry propios (no comparte BaseYangoApiService).
 */
@Slf4j
@Component
public class YangoClient {

    private static final int RETRY_401_DELAY_MS = 75;

    private final RestTemplate restTemplate;
    private final YangoCookiePool cookiePool;
    private final YegoProOpsProperties proOpsProperties;

    public YangoClient(
            @Qualifier("yangoExternalRestTemplate") RestTemplate restTemplate,
            YangoCookiePool cookiePool,
            YegoProOpsProperties proOpsProperties) {
        this.restTemplate = restTemplate;
        this.cookiePool = cookiePool;
        this.proOpsProperties = proOpsProperties;
        log.debug("[YangoClient] Inicializado cookies={} RestTemplate=pool(Yango external)", cookiePool.size());
    }

    private HttpHeaders headersSuggestions(String cookie, String parkId) {
        String pid = resolveParkId(parkId);
        String cookieConParkId = ajustarCookieParkId(cookie, pid);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", cookieConParkId != null ? cookieConParkId : cookie);
        headers.set("x-park-id", pid);
        headers.set("language", "es-419");
        headers.set("x-client-version", "fleet/19321");
        headers.set("origin", "https://fleet.yango.com");
        headers.set("accept-language", "es-419,es;q=0.9");
        return headers;
    }

    private HttpHeaders headersFleetJson(String cookie, String parkId) {
        String pid = resolveParkId(parkId);
        String cookieAjustado = ajustarCookieParkId(cookie, pid);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", cookieAjustado);
        headers.set("x-park-id", pid);
        headers.set("language", "es-419");
        headers.set("x-client-version", "fleet/19321");
        headers.set("origin", "https://fleet.yango.com");
        headers.set("accept-language", "es-419,es;q=0.9");
        return headers;
    }

    /** Misma convención que suggestions: cookie alineada con el parque de la cabecera (evita 401 por contexto inconsistente). */
    private static String ajustarCookieParkId(String cookie, String pid) {
        if (cookie == null || pid == null || pid.isBlank()) {
            return cookie;
        }
        if (cookie.contains("park_id=")) {
            return cookie.replaceFirst("park_id=[^;]+", "park_id=" + pid);
        }
        String trimmed = cookie.trim();
        return trimmed.endsWith(";") ? trimmed + " park_id=" + pid : trimmed + "; park_id=" + pid;
    }

    private String resolveParkId(String parkId) {
        return parkId != null && !parkId.isBlank() ? parkId : proOpsProperties.getParkId();
    }

    /**
     * POST suggestions/list — conexión directa (sin proxy) para menor latencia.
     */
    public ResponseEntity<String> postSuggestions(String bodyJson, String parkId) throws Exception {
        return exchangeDirect(proOpsProperties.getYango().getSuggestionsUrl(), HttpMethod.POST, bodyJson, cookie -> headersSuggestions(cookie, parkId));
    }

    /**
     * POST a endpoints fleet (income, driver details, etc.) con cabeceras estándar.
     */
    public ResponseEntity<String> postFleet(String url, String bodyJson, String parkId) throws Exception {
        return exchangeDirect(url, HttpMethod.POST, bodyJson, cookie -> headersFleetJson(cookie, parkId));
    }

    /**
     * GET goals — conexión directa.
     */
    public ResponseEntity<String> getGoals(String url, String parkId) throws Exception {
        return exchangeDirect(url, HttpMethod.GET, null, cookie -> headersFleetJson(cookie, parkId));
    }

    private <T> ResponseEntity<String> exchangeDirect(
            String url, HttpMethod method, T requestBody,
            java.util.function.Function<String, HttpHeaders> headersFunc) throws Exception {
        Exception ultimoError = null;
        for (int attempt = 0; attempt < cookiePool.size(); attempt++) {
            int i = cookiePool.randomValidIndex();
            if (i < 0) {
                cookiePool.resetInvalid();
                break;
            }
            String cookie = cookiePool.cookieAt(i);
            HttpHeaders headers = headersFunc.apply(cookie);
            try {
                HttpEntity<T> req = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> resp = restTemplate.exchange(url, method, req, String.class);
                if (resp.getStatusCode().is2xxSuccessful()) return resp;
                return resp;
            } catch (HttpClientErrorException e) {
                ultimoError = e;
                int code = e.getStatusCode().value();
                // Solo 401 = sesión inválida. 403 (no_permissions) suele ser rol/recurso, no tirar la cookie del pool.
                if (code == 401) {
                    cookiePool.markInvalid(i);
                }
                if (code == 401 || code == 403 || code == 429) {
                    delay(RETRY_401_DELAY_MS);
                    continue;
                }
                throw e;
            } catch (Exception e) {
                ultimoError = e;
                delay(RETRY_401_DELAY_MS);
                continue;
            }
        }
        cookiePool.resetInvalid();
        if (ultimoError != null) throw ultimoError;
        throw new RuntimeException("YangoClient: todas las cookies fallaron");
    }

    private static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
