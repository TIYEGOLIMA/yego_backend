package com.yego.backend.service.yego_pro_ops.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.config.yego_pro_ops.ProxyConfig;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Slf4j
public abstract class BaseYangoApiService {

    protected final RestTemplate restTemplate;
    protected final RestTemplate yangoProxyRestTemplate;
    protected final ProxyConfig proxyConfig;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected static final String PARK_ID = "64085dd85e124e2c808806f70d527ea8";

    protected static final String YANGO_API_URL = "https://fleet.yango.com/api/fleet/map/v2/drivers/points";
    protected static final String YANGO_DRIVERS_LIST_API_URL = "https://fleet.yango.com/api/fleet/map/v1/drivers/list";
    protected static final String YANGO_CONTRACTORS_API_URL = "https://fleet.yango.com/api/fleet/contractor-profiles-manager/v2/contractors/list";
    protected static final String YANGO_GPS_API_URL = "https://fleet.yango.com/api/fleet/map/v1/driver/gps";
    protected static final String YANGO_DRIVER_INCOME_API_URL = "https://fleet.yango.com/api/v1/cards/driver/income";
    protected static final String YANGO_ORDERS_API_URL = "https://fleet.yango.com/api/reports-api/v1/orders/list";
    protected static final String YANGO_SUGGESTIONS_LIST_URL = "https://fleet.yango.com/api/fleet/contractor-profiles-manager/v1/suggestions/list";

    private static final List<String> COOKIES_POOL = Arrays.asList(
        "i=NYOL+ROg0yOUvyEaPCQvN4DHZpVjMKwlVZbprlYyJGpjRRgUVReA08a73Wz1/FS0VUYk8swbiEoZmlZLLsl5OJawROo=; yandexuid=9517632661779283577; yashr=5886161291779283577; yuidss=9517632661779283577; receive-cookie-deprecation=1; gdpr=0; _ym_uid=177928358264702995; _ym_d=1779283582; Session_id=3:1779283852.5.0.1779283852879:WbD9Jg:f51c.1.2:1|2015824474.0.2.0:3.3:1779283852|60:11916660.525693.MHjfrO1ZynP3sIRh7nbwjdzxcng; sessar=1.1844558.CiBWgwV9WHcOheyQlwHOS15Drmyxz4fR8Xf2F8RYOYw8Cw.AyYge7M0IMvJmym-tH_qFz52SCYjUKvDYFeKw1zi1sk; sessionid2=3:1779283852.5.0.1779283852879:WbD9Jg:f51c.1.2:1|2015824474.0.2.0:3.3:1779283852|60:11916660.525693.fakesign0000000000000000000; ys=udn.cDpKaGFqYWlyYS5vY2hvYQ%3D%3D; L=XBlmXwRcdnxiAQdxfFdWA0xcTFF1VGZweAwVLzU/F1ZJGwIbXCI=.1779283852.1908700.371733.9fa88420a28d18b439681914f49ffdde; yandex_login=Jhajaira.ochoa; _ym_isad=2; yp=2094643852.udn.cDpKaGFqYWlyYS5vY2hvYQ%3D%3D#1780402270.yu.9517632661779283577; ymex=1782907870.oyu.9517632661779283577#2094643579.yrts.1779283579; _ym_visorc=b; _yasc=yZ7fY77AKah+5JYEFH/t+iMvTa0gUyYw+RU1mgoV2Z5oPfYPgVlRIRBPINmDaOk6AW4zMyIrIQ==.MTc3OTc0NjQ4NzcwMg==; bh=Ej8iQ2hyb21pdW0iO3Y9IjE0OCIsIkdvb2dsZSBDaHJvbWUiO3Y9IjE0OCIsIk5vdC9BKUJyYW5kIjt2PSI5OSIaA3g4NiIOMTQ4LjAuNzc3OC4yMTYqAj8wOgdXaW5kb3dzQgYxMC4wLjBKAjY0UlsiQ2hyb21pdW0iO3Y9IjE0OC4wLjc3NzguMjE2IiwiR29vZ2xlIENocm9tZSI7dj0iMTQ4LjAuNzc3OC4yMTYiLCJOb3QvQSlCcmFuZCI7dj0iOTkuMC4wLjAiYISM9tAGah7cyuH/CJLYobEDn8/h6gP7+vDnDev//fYP06DOhwg=",
        "i=UJLIEwSC6oKbdVhq5O7kwifboecYIauewX8lfuf+iWnOFsEuohMnnZQxTltyQTiTnx9UTcIdYZLNhYGdE9gikSiSxBw=; yandexuid=7615489531765348120; yashr=8990445271765348120; yuidss=7615489531765348120; ymex=2080708123.yrts.1765348123; _ym_uid=1765348122485351205; _ym_d=1765348124; gdpr=0; yandex_login=giomarortega; Session_id=3:1778965558.5.0.1778965558025:ymIZJg:9b00.1.2:1|2223153146.0.2.0:3.3:1778965558|60:11908555.623.UCX3hVYBx_3QdWRzKGYbYKVjfTI; sessar=1.1844558.CiDeDwn92ZY5Umg1fgw3OyU_qjaBG_VxzlrBtryMaEuV9w.3EK73c9n7y9BK0TWABVJOx9_4LsJTBWt-m9_88tReW4; sessionid2=3:1778965558.5.0.1778965558025:ymIZJg:9b00.1.2:1|2223153146.0.2.0:3.3:1778965558|60:11908555.623.fakesign0000000000000000000; yp=2094325558.udn.cDpnaW9tYXJvcnRlZ2E%3D; ys=udn.cDpnaW9tYXJvcnRlZ2E%3D; L=SRJJYER1Qkd3YHRCRkEKAm9HZVlRfkcCJFAIKSc5Wzk8BgEL.1778965558.1900056.366277.0e503dca0e3d95265da89538ab541081; park_id=08e20910d81d42658d4334d3f6d10ac0; _yasc=gKwWtJ3rebbvfVhB6dS4xeJhMLoNKtbU5BAH79KWtSxXLOZHsiXFjNN8B8pNqMX84HEWUeD2.MTc3OTU4NTI1Mzg5Mw==; _ym_isad=2; _ym_visorc=b; bh=EkIiQ2hyb21pdW0iO3Y9IjE0OCIsICJNaWNyb3NvZnQgRWRnZSI7dj0iMTQ4IiwgIk5vdC9BKUJyYW5kIjt2PSI5OSIaA2FybSINMTQ4LjAuMzk2Ny44MyoCPzE6CSJBbmRyb2lkIkIGMjYuMC4wSgI2NFJbIkNocm9taXVtIjt2PSIxNDguMC43Nzc4LjE4MCIsIk1pY3Jvc29mdCBFZGdlIjt2PSIxNDguMC4zOTY3LjgzIiwiTm90L0EpQnJhbmQiO3Y9Ijk5LjAuMC4wImCj9/LQBmol3Mql7AbPn4yfBaynvLsFoJ3s6wP8ua//B9/9z4IJ5bXNhwjyVA==",
        "yandexuid=6009311931761677705; yashr=4567181961761677705; yuidss=6009311931761677705; receive-cookie-deprecation=1; gdpr=0; _ym_uid=1761677706290870939; _ym_d=1761677707; i=WPv45DbbiaiQTfOesurzPwPDcYTOSOoBsoiMqnCbM8UNdwnBPqcEVeWPbr+/nREEJsBHtNGb/FFdfO9HLKe33wC900U=; Session_id=3:1767916938.5.0.1761677775316:WbD9Jg:71df.1.2:1|1782860170.0.2.0:3.3:1761677775|2220343194.-1.0.0:3.2:2426755.3:1764104530|60:11590429.497800.3oxV2BfdArdTZgb0iJuNpsl7pTQ; sessar=1.1504434.CiBoQiEtUqB8Clq12Z20uRTAKOBx8rNaYp3ayAviftuOlQ.HmHoWlhWBAmESc7SsPOk0_fwoYScX1OCMb7N5WOch3w; sessionid2=3:1767916938.5.0.1761677775316:WbD9Jg:71df.1.2:1|1782860170.0.2.0:3.3:1761677775|2220343194.-1.0.0:3.2:2426755.3:1764104530|60:11590429.497800.fakesign0000000000000000000; L=cwNkZHx2bUdLfgZ1eX1WBUJKR0J7dVdUATclGQI2VgomGwMkLic=.1767916938.1586619.346423.d1431f8e0fe9a8126fdb675787bc79fb; yandex_login=gonzalofajardo; park_id=08e20910d81d42658d4334d3f6d10ac0; _ym_isad=2; yp=2079464530.multib.1#2083276938.udn.cDpHb256YWxvIEZhamFyZG8%3D#1768500383.yu.6009311931761677705; ymex=1771005983.oyu.6009311931761677705#2077037707.yrts.1761677707; _ym_visorc=w; _yasc=2mKcY8GygbjykCQel1V9urGg+aqTEZmAqVNXKfK1JjKMIohuQ3hRqw7ff6w9h+pemrcG; bh=EkEiR29vZ2xlIENocm9tZSI7dj0iMTQzIiwgIkNocm9taXVtIjt2PSIxNDMiLCAiTm90IEEoQnJhbmQiO3Y9IjI0IhoDeDg2Ig4xNDMuMC43NDk5LjE5MyoCPzA6CSJXaW5kb3dzIkIGMTkuMC4wSgI2NFJbIkdvb2dsZSBDaHJvbWUiO3Y9IjE0My4wLjc0OTkuMTkzIiwiQ2hyb21pdW0iO3Y9IjE0My4wLjc0OTkuMTkzIiwiTm90IEEoQnJhbmQiO3Y9IjI0LjAuMC4wImCjtb/LBmoe3Mrh/wiS2KGxA5/P4eoD+/rw5w3r//32D/vMzYcI"
    );

    private static final Random RANDOM = new Random();
    private static final Set<Integer> COOKIE_INDICES_INVALIDOS = ConcurrentHashMap.newKeySet();
    private static final int RETRY_401_DELAY_MS = 150;
    private static final int RETRY_PROXY_DELAY_MS = 800;
    private static final long DEFAULT_THROTTLE_MS = 5_000L;

    private static final ConcurrentHashMap<Integer, AtomicLong> ULTIMA_LLAMADA_POR_COOKIE = new ConcurrentHashMap<>();

    protected BaseYangoApiService(
            RestTemplate restTemplate,
            @Qualifier("yangoProxyRestTemplate") RestTemplate yangoProxyRestTemplate,
            ProxyConfig proxyConfig) {
        this.restTemplate = restTemplate;
        this.yangoProxyRestTemplate = yangoProxyRestTemplate;
        this.proxyConfig = proxyConfig;
        log.debug("[BaseYangoApi] {} listo (cookies={} proxy={})",
            getClass().getSimpleName(), COOKIES_POOL.size(),
            proxyConfig != null && proxyConfig.isEnabled());
    }

    private RestTemplate getRestTemplate() {
        return proxyConfig != null && proxyConfig.isEnabled() && yangoProxyRestTemplate != null
            ? yangoProxyRestTemplate
            : restTemplate;
    }

    protected static void marcarCookieInvalida(int index) {
        if (index >= 0 && index < COOKIES_POOL.size()) {
            COOKIE_INDICES_INVALIDOS.add(index);
            log.debug("[BaseYangoApi] cookie #{} marcada inválida", index + 1);
        }
    }

    protected static int obtenerIndiceCookieValida() {
        List<Integer> validos = new ArrayList<>(COOKIES_POOL.size());
        for (int i = 0; i < COOKIES_POOL.size(); i++) {
            if (!COOKIE_INDICES_INVALIDOS.contains(i)) validos.add(i);
        }
        return validos.isEmpty() ? -1 : validos.get(RANDOM.nextInt(validos.size()));
    }

    private static int obtenerIndiceCookieMenosUsada(long throttleMs) {
        int mejor = -1;
        long mejorTimestamp = Long.MAX_VALUE;
        for (int i = 0; i < COOKIES_POOL.size(); i++) {
            if (COOKIE_INDICES_INVALIDOS.contains(i)) continue;
            long ts = ULTIMA_LLAMADA_POR_COOKIE.computeIfAbsent(i, k -> new AtomicLong(0)).get();
            if (ts < mejorTimestamp) {
                mejorTimestamp = ts;
                mejor = i;
            }
        }
        if (mejor < 0) return -1;
        long espera = throttleMs - (System.currentTimeMillis() - mejorTimestamp);
        if (espera > 0) sleepSafe(espera);
        ULTIMA_LLAMADA_POR_COOKIE.get(mejor).set(System.currentTimeMillis());
        return mejor;
    }

    private static void sleepSafe(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected String obtenerCookiePorIndice(int index) {
        if (index < 0 || index >= COOKIES_POOL.size()) {
            int valido = obtenerIndiceCookieValida();
            return valido >= 0 ? COOKIES_POOL.get(valido) : COOKIES_POOL.get(0);
        }
        return COOKIES_POOL.get(index);
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

        for (int attempt = 0; attempt < COOKIES_POOL.size(); attempt++) {
            int i = obtenerIndiceCookieMenosUsada(throttleMs);
            if (i < 0) {
                COOKIE_INDICES_INVALIDOS.clear();
                log.warn("[BaseYangoApi] todas las cookies inválidas, reset del pool");
                break;
            }
            HttpHeaders headers = headersFunc.apply(COOKIES_POOL.get(i));

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

        COOKIE_INDICES_INVALIDOS.clear();
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
        headers.set("x-park-id", PARK_ID);
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
        headers.set("x-park-id", PARK_ID);
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
        headers.set("x-park-id", parkId != null ? parkId : PARK_ID);
        headers.set("language", "es-419");
        headers.set("x-client-version", "fleet/19321");
        headers.set("origin", "https://fleet.yango.com");
        headers.set("accept-language", "es-419,es;q=0.9");
        return headers;
    }

    public void warmupCookiePool() {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("park_id", PARK_ID);
            body.put("car", Collections.emptyMap());
            body.put("statuses", Arrays.asList("in_order", "free"));
            ejecutarConRetryCookies(YANGO_API_URL, HttpMethod.POST, body, this::crearHeadersDriversPointsConCookie);
        } catch (Exception e) {
            log.warn("[BaseYangoApi] warmup cookies falló: {}", e.getMessage());
        }
    }
}
