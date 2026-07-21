package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.config.yego_pro_ops.ProxyConfig;
import com.yego.backend.config.yego_pro_ops.YegoProOpsProperties;
import com.yego.backend.integration.YangoCookiePool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public abstract class BaseYangoApiService {

    protected final RestTemplate restTemplate;
    protected final RestTemplate yangoProxyRestTemplate;
    protected final ProxyConfig proxyConfig;
    protected final ObjectMapper objectMapper;
    protected final YegoProOpsProperties proOpsProperties;

    private static final int RETRY_401_DELAY_MS = 150;
    private static final int RETRY_PROXY_DELAY_MS = 800;
    private static final long DEFAULT_THROTTLE_MS = 5_000L;
    private final YangoCookiePool cookiePool;

    protected BaseYangoApiService(
            RestTemplate restTemplate,
            @Qualifier("yangoProxyRestTemplate") RestTemplate yangoProxyRestTemplate,
            ProxyConfig proxyConfig,
            YangoCookiePool cookiePool,
            ObjectMapper objectMapper,
            YegoProOpsProperties proOpsProperties) {
        this.restTemplate = restTemplate;
        this.yangoProxyRestTemplate = yangoProxyRestTemplate;
        this.proxyConfig = proxyConfig;
        this.cookiePool = cookiePool;
        this.objectMapper = objectMapper;
        this.proOpsProperties = proOpsProperties;
        log.debug("[BaseYangoApi] {} listo (cookies={} proxy={})",
            getClass().getSimpleName(), cookiePool.size(),
            proxyConfig != null && proxyConfig.isEnabled());
    }

    private RestTemplate getRestTemplate() {
        return proxyConfig != null && proxyConfig.isEnabled() && yangoProxyRestTemplate != null
            ? yangoProxyRestTemplate
            : restTemplate;
    }

    protected void marcarCookieInvalida(int index) {
        cookiePool.markInvalid(index);
        log.debug("[BaseYangoApi] cookie #{} marcada inválida", index + 1);
    }

    protected int obtenerIndiceCookieValida() {
        return cookiePool.randomValidIndex();
    }

    private int obtenerIndiceCookieMenosUsada(long throttleMs) {
        return cookiePool.reserveLeastRecentlyUsed(throttleMs);
    }

    protected String obtenerCookiePorIndice(int index) {
        if (index < 0 || index >= cookiePool.size()) {
            int valido = obtenerIndiceCookieValida();
            return cookiePool.cookieAt(valido);
        }
        return cookiePool.cookieAt(index);
    }

    protected <T> ResponseEntity<String> ejecutarConRetryCookies(
            String url,
            HttpMethod method,
            T requestBody,
            Function<String, HttpHeaders> headersFunc) throws Exception {
        return ejecutarConRetryCookies(url, method, requestBody, headersFunc, DEFAULT_THROTTLE_MS);
    }

    protected <T> ResponseEntity<String> ejecutarConRetryCookies(
            String url,
            HttpMethod method,
            T requestBody,
            Function<String, HttpHeaders> headersFunc,
            long throttleMs) throws Exception {

        Exception ultimoError = null;

        for (int attempt = 0; attempt < cookiePool.size(); attempt++) {
            int i = obtenerIndiceCookieMenosUsada(throttleMs);
            if (i < 0) {
                cookiePool.resetInvalid();
                log.warn("[BaseYangoApi] todas las cookies inválidas, reset del pool");
                break;
            }
            HttpHeaders headers = headersFunc.apply(cookiePool.cookieAt(i));

            try {
                HttpEntity<T> request = new HttpEntity<>(requestBody, headers);
                return getRestTemplate().exchange(url, method, request, String.class);
            } catch (HttpClientErrorException e) {
                ultimoError = e;
                int status = e.getStatusCode().value();
                String body = e.getResponseBodyAsString();
                if (esErrorProxy(body)) {
                    log.warn("[BaseYangoApi] error proxy cookie #{}: {}", i + 1, status);
                    delay(RETRY_PROXY_DELAY_MS);
                    continue;
                }
                if (status == 401 || status == 403 || status == 429) {
                    marcarCookieInvalida(i);
                    delay(RETRY_401_DELAY_MS);
                    continue;
                }
                throw e;
            } catch (HttpServerErrorException e) {
                ultimoError = e;
                log.warn("[BaseYangoApi] 5xx con cookie #{}: {}", i + 1, e.getStatusCode().value());
                delay(RETRY_401_DELAY_MS);
            } catch (Exception e) {
                ultimoError = e;
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                if (msg.toLowerCase().contains("proxy") || msg.contains("407")) {
                    delay(RETRY_PROXY_DELAY_MS);
                } else {
                    delay(RETRY_401_DELAY_MS);
                }
            }
        }

        cookiePool.resetInvalid();
        log.error("[BaseYangoApi] todas las cookies fallaron url={}", url);
        if (ultimoError != null) throw ultimoError;
        throw new RuntimeException("Todas las cookies fallaron");
    }

    private static boolean esErrorProxy(String body) {
        return body != null && (body.contains("proxy") || body.contains("Proxy")
            || body.contains("update your proxy address")
            || body.contains("proxy username") || body.contains("proxy port"));
    }

    private static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    protected HttpHeaders crearHeadersConCookie(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", cookie);
        headers.set("x-park-id", proOpsProperties.getParkId());
        headers.set("language", "es-419");
        headers.set("x-client-version", "fleet/19321");
        headers.set("origin", "https://fleet.yango.com");
        headers.set("accept-language", "es-419,es;q=0.9");
        return headers;
    }

    protected HttpHeaders crearHeadersDriversPointsConCookie(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", cookie);
        headers.set("x-park-id", proOpsProperties.getParkId());
        headers.set("origin", "https://fleet.yango.com");
        return headers;
    }

    protected HttpHeaders crearHeadersSuggestionsConCookieYParkId(String cookie, String parkId) {
        String cookieFinal = cookie != null && cookie.contains("park_id=")
            ? cookie.replaceFirst("park_id=[^;]+", "park_id=" + parkId)
            : cookie;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", cookieFinal != null ? cookieFinal : cookie);
        headers.set("x-park-id", parkId != null ? parkId : proOpsProperties.getParkId());
        headers.set("language", "es-419");
        headers.set("x-client-version", "fleet/19321");
        headers.set("origin", "https://fleet.yango.com");
        headers.set("accept-language", "es-419,es;q=0.9");
        return headers;
    }

    public void warmupCookiePool() {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("park_id", proOpsProperties.getParkId());
            body.put("car", Collections.emptyMap());
            body.put("statuses", Arrays.asList("in_order", "free"));
            ejecutarConRetryCookies(proOpsProperties.getYango().getDriverPointsUrl(), HttpMethod.POST, body, this::crearHeadersDriversPointsConCookie);
        } catch (Exception e) {
            log.warn("[BaseYangoApi] warmup cookies falló: {}", e.getMessage());
        }
    }
}
