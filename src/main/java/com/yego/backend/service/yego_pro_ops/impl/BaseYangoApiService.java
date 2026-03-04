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

/**
 * Clase base abstracta para servicios que interactúan con la API de Yango
 * Centraliza la lógica común: RestTemplate, headers, throttling, cookies
 */
@Slf4j
public abstract class BaseYangoApiService {
    
    protected final RestTemplate restTemplate;
    protected final RestTemplate yangoProxyRestTemplate;
    protected final ProxyConfig proxyConfig;
    protected final ObjectMapper objectMapper = new ObjectMapper();
    
    // Constantes compartidas
    protected static final String PARK_ID = "64085dd85e124e2c808806f70d527ea8";
    
    // URLs de API de Yango
    protected static final String YANGO_API_URL = "https://fleet.yango.com/api/fleet/map/v2/drivers/points";
    protected static final String YANGO_DRIVERS_LIST_API_URL = "https://fleet.yango.com/api/fleet/map/v1/drivers/list";
    protected static final String YANGO_CONTRACTORS_API_URL = "https://fleet.yango.com/api/fleet/contractor-profiles-manager/v2/contractors/list";
    protected static final String YANGO_GPS_API_URL = "https://fleet.yango.com/api/fleet/map/v1/driver/gps";
    protected static final String YANGO_DRIVER_INCOME_API_URL = "https://fleet.yango.com/api/v1/cards/driver/income";
    protected static final String YANGO_WORK_RULES_API_URL = "https://fleet.yango.com/api/fleet/driver-work-rules/v1/work-rules/light-list";
    protected static final String YANGO_ORDERS_API_URL = "https://fleet.yango.com/api/reports-api/v1/orders/list";
    
    // Pool de cookies para rotación aleatoria
    private static final List<String> COOKIES_POOL = Arrays.asList(
        // Cookie 1: Jhajaira.ochoa
        "yashr=299832191758141387; receive-cookie-deprecation=1; gdpr=0; _ym_uid=175814138831171998; _ym_d=1758141396; yandexuid=9201743261758137514; yuidss=9201743261758137514; yandex_login=Jhajaira.ochoa; L=ZxtXZnxqBmR/SAVhdVZyXwJfaA9cBV1cLlAoE1ddHRAZOQxQLTM=.1759148388.1235855.353634.d47e5d7bafd679e1c83d4f42c6e23cd9; Session_id=3:1764107903.5.0.1758141420632:WbD9Jg:3a2a.1.2:1|2015824474.1006968.2.0:3.2:1006968.3:1759148388|60:11433100.181615.QwZv5z-R92wHzh4j43-daY0-K7g; sessar=1.1396519.CiDPtjUqckvAWEzdml3-Xvlj9hjrxSX6tJqOaxrdeZn5IA.jv1_Nvn5vO56j4mwzyFG1Wg8pHX_1BWCf-tAOHo6bQk; sessionid2=3:1764107903.5.0.1758141420632:WbD9Jg:3a2a.1.2:1|2015824474.1006968.2.0:3.2:1006968.3:1759148388|60:11433100.181615.fakesign0000000000000000000; i=XLXWMoCXAOgX6hzpx/AmT+HOGGwAwQhiTRKOzxl2tkMXu90DChcwoTT5z8qvZlmQyhkwZXurYZsuUa9AHb5foPXF2Rc=; _ym_isad=2; yp=2074508388.udn.cDpKaGFqYWlyYS5vY2hvYQ%3D%3D#1764940883.yu.9201743261758137514; ymex=1767446483.oyu.9201743261758137514#2073501389.yrts.1758141389; _ym_visorc=b; bh=Ej8iQ2hyb21pdW0iO3Y9IjE0MiIsIkdvb2dsZSBDaHJvbWUiO3Y9IjE0MiIsIk5vdF9BIEJyYW5kIjt2PSI5OSIaA3g4NiIOMTQyLjAuNzQ0NC4xNzYqAj8wOgdXaW5kb3dzQgYxMC4wLjBKAjY0UlsiQ2hyb21pdW0iO3Y9IjE0Mi4wLjc0NDQuMTc2IiwiR29vZ2xlIENocm9tZSI7dj0iMTQyLjAuNzQ0NC4xNzYiLCJOb3RfQSBCcmFuZCI7dj0iOTkuMC4wLjAiYPzux8kGah7cyuH/CJLYobEDn8/h6gP7+vDnDev//fYP+JzMhwg=; _yasc=fAS4rNTIoRKZq+JMg4Gaj9eUc5ZA62kxyJug7jaq8e08yG17i2jy7kvgtoikFrhpuyUc",
        
        // Cookie 2: giomarortega (actualizada)
        "i=nOvRJ2K/cZwWkUf8kHuUrHWLCzY44glVytXeWCF8UjS0SnNco2YVTIDg8AGF5VTgKfZpWwEgwj6jgfVAZT4CTM4GaRw=; yandexuid=2622840871772483002; yashr=7273643541772483002; yuidss=2622840871772483002; ymex=2087843007.yrts.1772483007; _yasc=H4vwUYsVB+r9GFV458dHFavRAjYyBFGrHkbwL4nBaMOTQpvR9guDabyjkFlc83Pc; gdpr=0; _ym_uid=1772483007632502910; _ym_d=1772483008; _ym_visorc=b; _ym_isad=2; Session_id=3:1772483086.5.0.1772483086636:WbD9Jg:6f7d.1.2:1|2223153146.0.2.0:3.3:1772483086|60:11753585.533197.wHgwORMpF9EKFQj7-SK2AVwmQbk; sessar=1.1719226.CiBRedxCvhmLjGyB74texUepBXRLzfY-1FZJSSO-qfJqgA.Y43nlzzfd6YKZ0L2ylK-8770uQK6vm1YPkmHiCM3JR4; sessionid2=3:1772483086.5.0.1772483086636:WbD9Jg:6f7d.1.2:1|2223153146.0.2.0:3.3:1772483086|60:11753585.533197.fakesign0000000000000000000; yp=2087843086.udn.cDpnaW9tYXJvcnRlZ2E%3D; ys=udn.cDpnaW9tYXJvcnRlZ2E%3D; L=Sg1hU15fdAB8TXVDWmJAWwhXZAZjAnBhPSQfWBcZWAMMB1Us.1772483086.1745990.328358.659e3758f20db470aa4cc2f40c1ef89b; yandex_login=giomarortega; bh=EkEiTm90KEE6QnJhbmQiO3Y9IjgiLCAiQ2hyb21pdW0iO3Y9IjE0NCIsICJNaWNyb3NvZnQgRWRnZSI7dj0iMTQ0IioCPzE6CSJBbmRyb2lkImDq5JfNBmom3Mql7AbPn4yfBaynvLsFoJ3s6wP8ua//B9/9+9wH5bXNhwi/gwM=",
        
        // Cookie 3: gonzalofajardo (nueva)
        "yandexuid=6009311931761677705; yashr=4567181961761677705; yuidss=6009311931761677705; receive-cookie-deprecation=1; gdpr=0; _ym_uid=1761677706290870939; _ym_d=1761677707; i=WPv45DbbiaiQTfOesurzPwPDcYTOSOoBsoiMqnCbM8UNdwnBPqcEVeWPbr+/nREEJsBHtNGb/FFdfO9HLKe33wC900U=; Session_id=3:1767916938.5.0.1761677775316:WbD9Jg:71df.1.2:1|1782860170.0.2.0:3.3:1761677775|2220343194.-1.0.0:3.2:2426755.3:1764104530|60:11590429.497800.3oxV2BfdArdTZgb0iJuNpsl7pTQ; sessar=1.1504434.CiBoQiEtUqB8Clq12Z20uRTAKOBx8rNaYp3ayAviftuOlQ.HmHoWlhWBAmESc7SsPOk0_fwoYScX1OCMb7N5WOch3w; sessionid2=3:1767916938.5.0.1761677775316:WbD9Jg:71df.1.2:1|1782860170.0.2.0:3.3:1761677775|2220343194.-1.0.0:3.2:2426755.3:1764104530|60:11590429.497800.fakesign0000000000000000000; L=cwNkZHx2bUdLfgZ1eX1WBUJKR0J7dVdUATclGQI2VgomGwMkLic=.1767916938.1586619.346423.d1431f8e0fe9a8126fdb675787bc79fb; yandex_login=gonzalofajardo; park_id=08e20910d81d42658d4334d3f6d10ac0; _ym_isad=2; yp=2079464530.multib.1#2083276938.udn.cDpHb256YWxvIEZhamFyZG8%3D#1768500383.yu.6009311931761677705; ymex=1771005983.oyu.6009311931761677705#2077037707.yrts.1761677707; _ym_visorc=w; _yasc=2mKcY8GygbjykCQel1V9urGg+aqTEZmAqVNXKfK1JjKMIohuQ3hRqw7ff6w9h+pemrcG; bh=EkEiR29vZ2xlIENocm9tZSI7dj0iMTQzIiwgIkNocm9taXVtIjt2PSIxNDMiLCAiTm90IEEoQnJhbmQiO3Y9IjI0IhoDeDg2Ig4xNDMuMC43NDk5LjE5MyoCPzA6CSJXaW5kb3dzIkIGMTkuMC4wSgI2NFJbIkdvb2dsZSBDaHJvbWUiO3Y9IjE0My4wLjc0OTkuMTkzIiwiQ2hyb21pdW0iO3Y9IjE0My4wLjc0OTkuMTkzIiwiTm90IEEoQnJhbmQiO3Y9IjI0LjAuMC4wImCjtp/LBmoe3Mrh/wiS2KGxA5/P4eoD+/rw5w3r//32D/vMzYcI"
    );
    
    private static final Random RANDOM = new Random();

    /** Índices de cookies que ya devolvieron 401: todos los hilos las evitan sin probar de nuevo */
    private static final Set<Integer> COOKIE_INDICES_INVALIDOS = ConcurrentHashMap.newKeySet();

    /** Delay corto en retry 401 (cambio de cookie) para no sumar segundos */
    private static final int RETRY_401_DELAY_MS = 150;
    /** Delay en error de proxy (conexión) */
    private static final int RETRY_PROXY_DELAY_MS = 800;
    
    // Throttling compartido
    protected final AtomicLong ultimaLlamadaTimestamp = new AtomicLong(0);
    
    protected BaseYangoApiService(
            RestTemplate restTemplate,
            @Qualifier("yangoProxyRestTemplate") RestTemplate yangoProxyRestTemplate,
            ProxyConfig proxyConfig) {
        this.restTemplate = restTemplate;
        this.yangoProxyRestTemplate = yangoProxyRestTemplate;
        this.proxyConfig = proxyConfig;
        
        // 🔍 PROBE: Verificación de inicialización
        log.info("✅ [BaseYangoApiService] {} inicializado correctamente - Pool de cookies: {} disponibles, Proxy: {}", 
            this.getClass().getSimpleName(), COOKIES_POOL.size(), 
            proxyConfig != null && proxyConfig.isEnabled() ? "HABILITADO" : "DESHABILITADO");
    }
    
    /**
     * Obtiene el RestTemplate a usar (con proxy si está habilitado)
     */
    protected RestTemplate getRestTemplate() {
        if (proxyConfig != null && proxyConfig.isEnabled() && yangoProxyRestTemplate != null) {
            return yangoProxyRestTemplate;
        }
        return restTemplate;
    }
    
    /**
     * Marca una cookie como inválida (401). El resto de hilos dejan de usarla de inmediato.
     */
    protected static void marcarCookieInvalida(int index) {
        if (index >= 0 && index < COOKIES_POOL.size()) {
            COOKIE_INDICES_INVALIDOS.add(index);
            log.info("🍪 [BaseYangoApiService] Cookie #{} marcada como inválida (compartido)", index + 1);
        }
    }

    /**
     * Devuelve un índice de cookie válido (no marcado como inválida), o -1 si todas están inválidas.
     */
    protected static int obtenerIndiceCookieValida() {
        List<Integer> validos = new ArrayList<>();
        for (int i = 0; i < COOKIES_POOL.size(); i++) {
            if (!COOKIE_INDICES_INVALIDOS.contains(i)) {
                validos.add(i);
            }
        }
        if (validos.isEmpty()) {
            return -1;
        }
        return validos.get(RANDOM.nextInt(validos.size()));
    }
    
    /**
     * Obtiene una cookie aleatoria del pool, evitando las marcadas como inválidas
     */
    protected String obtenerCookieAleatoria() {
        int index = obtenerIndiceCookieValida();
        return COOKIES_POOL.get(index);
    }
    
    /**
     * Obtiene una cookie específica del pool por índice
     */
    protected String obtenerCookiePorIndice(int index) {
        if (index < 0 || index >= COOKIES_POOL.size()) {
            return obtenerCookieAleatoria();
        }
        return COOKIES_POOL.get(index);
    }
    
    /**
     * Crea headers HTTP básicos con cookie aleatoria
     */
    protected HttpHeaders crearHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", obtenerCookieAleatoria());
        headers.set("x-park-id", PARK_ID);
        headers.set("language", "es-419");
        headers.set("x-client-version", "fleet/19321");
        headers.set("origin", "https://fleet.yango.com");
        headers.set("accept-language", "es-419,es;q=0.9");
        return headers;
    }
    
    /**
     * Crea headers HTTP básicos para drivers/points (sin language)
     */
    protected HttpHeaders crearHeadersDriversPoints() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", obtenerCookieAleatoria());
        headers.set("x-park-id", PARK_ID);
        headers.set("origin", "https://fleet.yango.com");
        return headers;
    }
    
    /**
     * Crea headers HTTP básicos para drivers/list
     */
    protected HttpHeaders crearHeadersDriversList() {
        return crearHeadersDriversPoints();
    }
    
    /**
     * Espera si es necesario para mantener un intervalo entre llamadas consecutivas a la API de Yango
     * @param delayMs Delay en milisegundos (default: 15 segundos)
     */
    protected void esperarSiEsNecesario(long delayMs) {
        long ahora = System.currentTimeMillis();
        long ultimaLlamada = ultimaLlamadaTimestamp.get();
        
        if (ultimaLlamada > 0) {
            long tiempoDesdeUltimaLlamada = ahora - ultimaLlamada;
            long tiempoAEsperar = delayMs - tiempoDesdeUltimaLlamada;
            
            if (tiempoAEsperar > 0) {
                log.debug("⏳ [BaseYangoApiService] Esperando {} ms antes de la siguiente llamada a Yango API", tiempoAEsperar);
                try {
                    Thread.sleep(tiempoAEsperar);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("⚠️ [BaseYangoApiService] Interrupción durante throttling", e);
                }
            }
        }
        
        ultimaLlamadaTimestamp.set(System.currentTimeMillis());
    }
    
    /**
     * Espera con delay por defecto (15 segundos)
     */
    protected void esperarSiEsNecesario() {
        esperarSiEsNecesario(15000);
    }
    
    /**
     * Ejecuta una llamada HTTP con retry automático usando rotación de cookies
     * Si falla con 429 (Too Many Requests) u otro error, intenta con otra cookie
     * 
     * @param url URL del endpoint
     * @param method Método HTTP (POST, GET, etc.)
     * @param requestBody Cuerpo de la petición (puede ser null)
     * @param headersFunc Función que crea headers con una cookie específica
     * @return ResponseEntity con la respuesta exitosa
     * @throws Exception Si todas las cookies fallan
     */
    protected <T> ResponseEntity<String> ejecutarConRetryCookies(
            String url,
            HttpMethod method,
            T requestBody,
            java.util.function.Function<String, HttpHeaders> headersFunc) throws Exception {
        
        Exception ultimoError = null;
        boolean retriedAfter401 = false;
        
        for (int attempt = 0; attempt < COOKIES_POOL.size(); attempt++) {
            int i = obtenerIndiceCookieValida();
            if (i < 0) {
                COOKIE_INDICES_INVALIDOS.clear();
                log.warn("🍪 [BaseYangoApiService] Todas las cookies marcadas inválidas; se reinicia el set para próximo intento");
                break;
            }
            String cookie = COOKIES_POOL.get(i);
            HttpHeaders headers = headersFunc.apply(cookie);
            
            try {
                HttpEntity<T> request = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = getRestTemplate().exchange(url, method, request, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    if (retriedAfter401) {
                        log.info("✅ [BaseYangoApiService] Retry exitoso con cookie #{}", i + 1);
                    }
                    return response;
                }
                
                log.warn("⚠️ [BaseYangoApiService] Respuesta no exitosa con cookie #{}: {}", i + 1, response.getStatusCode());
                return response;
                
            } catch (HttpClientErrorException e) {
                ultimoError = e;
                int statusCode = e.getStatusCode().value();
                String responseBody = e.getResponseBodyAsString();
                
                boolean esErrorProxy = responseBody != null && (
                    responseBody.contains("proxy") || responseBody.contains("Proxy") ||
                    responseBody.contains("update your proxy address") ||
                    responseBody.contains("proxy username") || responseBody.contains("proxy port")
                );
                
                if (esErrorProxy) {
                    log.error("❌ [BaseYangoApiService] Error de PROXY con cookie #{}: {}", i + 1, statusCode);
                    log.warn("🔄 [BaseYangoApiService] Intentando con siguiente cookie...");
                    delay(RETRY_PROXY_DELAY_MS);
                    continue;
                }
                
                if (statusCode == 401 || statusCode == 403 || statusCode == 429) {
                    String motivo = statusCode == 401 ? "Unauthorized (cookie expirada/inválida)"
                                     : statusCode == 403 ? "Forbidden" : "Too Many Requests";
                    log.warn("🔄 [BaseYangoApiService] Error {} con cookie #{} ({}); se marca inválida y se intenta con otra", 
                        statusCode, i + 1, motivo);
                    marcarCookieInvalida(i);
                    retriedAfter401 = true;
                    delay(RETRY_401_DELAY_MS);
                    continue;
                }
                log.error("❌ [BaseYangoApiService] Error HTTP {} con cookie #{}: {}", statusCode, i + 1, e.getMessage());
                throw e;
                
            } catch (HttpServerErrorException e) {
                ultimoError = e;
                log.warn("🔄 [BaseYangoApiService] Error del servidor {} con cookie #{}", e.getStatusCode().value(), i + 1);
                delay(RETRY_401_DELAY_MS);
                continue;
                
            } catch (Exception e) {
                ultimoError = e;
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                boolean esErrorProxy = msg.toLowerCase().contains("proxy") || msg.contains("407");
                if (esErrorProxy) {
                    log.error("❌ [BaseYangoApiService] Error de PROXY con cookie #{}: {}", i + 1, msg);
                    delay(RETRY_PROXY_DELAY_MS);
                } else {
                    log.warn("🔄 [BaseYangoApiService] Error con cookie #{}: {}", i + 1, msg);
                    delay(RETRY_401_DELAY_MS);
                }
                continue;
            }
        }
        
        COOKIE_INDICES_INVALIDOS.clear();
        log.error("❌ [BaseYangoApiService] Todas las {} cookies fallaron para URL: {}", COOKIES_POOL.size(), url);
        if (ultimoError != null) {
            throw ultimoError;
        }
        throw new RuntimeException("Todas las cookies fallaron");
    }
    
    private static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Crea headers con una cookie específica (para usar en retry)
     */
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
    
    /**
     * Crea headers para drivers/points con una cookie específica (para usar en retry)
     */
    protected HttpHeaders crearHeadersDriversPointsConCookie(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", cookie);
        headers.set("x-park-id", PARK_ID);
        headers.set("origin", "https://fleet.yango.com");
        return headers;
    }
    
    /**
     * Crea headers para drivers/list con una cookie específica (para usar en retry)
     */
    protected HttpHeaders crearHeadersDriversListConCookie(String cookie) {
        return crearHeadersDriversPointsConCookie(cookie);
    }

    /**
     * Hace una petición de prueba para validar/rotar cookies antes del paralelismo.
     * Si la cookie actual devuelve 401, se marca como inválida y se prueba la siguiente;
     * así el resto de hilos no pierden tiempo con la misma cookie.
     */
    public void warmupCookiePool() {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("park_id", PARK_ID);
            body.put("car", Collections.emptyMap());
            body.put("statuses", Arrays.asList("in_order", "free"));
            ejecutarConRetryCookies(YANGO_API_URL, HttpMethod.POST, body, this::crearHeadersDriversPointsConCookie);
            log.debug("🍪 [BaseYangoApiService] Warm-up de cookies OK");
        } catch (Exception e) {
            log.warn("🍪 [BaseYangoApiService] Warm-up de cookies falló (se usará retry en siguientes peticiones): {}", e.getMessage());
        }
    }
}

