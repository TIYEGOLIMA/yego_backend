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

import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
        
        // Cookie 2: giomarortega
        "i=x5tkbBS7C7HE+NXGcad3ZssQ3gf1F0rq356OWQvEx3ZB8N6sRw3Cgl6OxfwzvxG4EEjzwDu2xiGfC575M7+qz6ox3wc=; yandexuid=196877061764616562; yashr=2270601791764616562; yuidss=196877061764616562; ymex=2079976564.yrts.1764616564; receive-cookie-deprecation=1; gdpr=0; _ym_uid=1764616564116282218; _ym_d=1764616565; Session_id=3:1764616812.5.0.1764616812843:WbD9Jg:9933.1.2:1|2223153146.0.2.0:3.3:1764616812|60:11454337.136939.hHJxPhpQO1T97Iog_aHQCOuvpQo; sessar=1.1396519.CiCR_wLdjC3OTrDh2hgMr8--C-fwizMwlP9jW-dd6vGgRw.9KD2YMUjfA4ZbhzmsFVhHJOx2zEo94hMFlT83twWhyo; sessionid2=3:1764616812.5.0.1764616812843:WbD9Jg:9933.1.2:1|2223153146.0.2.0:3.3:1764616812|60:11454337.136939.fakesign0000000000000000000; yp=2079976812.udn.cDpnaW9tYXJvcnRlZ2E%3D; L=BBBBQ18BXmZ2XmtITlJ8VUBfcUBgeGFSPTgiLAs7KTUuNQkx.1764616812.1447419.396095.0920cd88815bbde83a0318732f9a8b82; yandex_login=giomarortega; _ym_isad=2; _yasc=GQ2XCBpQDzLhff5lrRlrxuaOh4LeuP1795j4xB9obQ+6KvYMs16SxDDeknIkX93UcXa/; bh=EjkiQ2hyb21pdW0iO3Y9IjE0MiIsICJCcmF2ZSI7dj0iMTQyIiwgIk5vdF9BIEJyYW5kIjt2PSI5OSIaA3g4NiIJMTQyLjAuMC4wKgI/MDoHIkxpbnV4IkIGNi4xNy40SgI2NFJJIkNocm9taXVtIjt2PSIxNDIuMC4wLjAiLCJCcmF2ZSI7dj0iMTQyLjAuMC4wIiwiTm90X0EgQnJhbmQiO3Y9Ijk5LjAuMC4wImCi1cLJBmoZ3MrpiA7yrLelC/v68OcN6//99g/4nMyHCA==",
        
        // Cookie 3: gonzalofajardo (nueva)
        "yandexuid=6009311931761677705; yashr=4567181961761677705; yuidss=6009311931761677705; receive-cookie-deprecation=1; gdpr=0; _ym_uid=1761677706290870939; _ym_d=1761677707; i=WPv45DbbiaiQTfOesurzPwPDcYTOSOoBsoiMqnCbM8UNdwnBPqcEVeWPbr+/nREEJsBHtNGb/FFdfO9HLKe33wC900U=; Session_id=3:1767916938.5.0.1761677775316:WbD9Jg:71df.1.2:1|1782860170.0.2.0:3.3:1761677775|2220343194.-1.0.0:3.2:2426755.3:1764104530|60:11590429.497800.3oxV2BfdArdTZgb0iJuNpsl7pTQ; sessar=1.1504434.CiBoQiEtUqB8Clq12Z20uRTAKOBx8rNaYp3ayAviftuOlQ.HmHoWlhWBAmESc7SsPOk0_fwoYScX1OCMb7N5WOch3w; sessionid2=3:1767916938.5.0.1761677775316:WbD9Jg:71df.1.2:1|1782860170.0.2.0:3.3:1761677775|2220343194.-1.0.0:3.2:2426755.3:1764104530|60:11590429.497800.fakesign0000000000000000000; L=cwNkZHx2bUdLfgZ1eX1WBUJKR0J7dVdUATclGQI2VgomGwMkLic=.1767916938.1586619.346423.d1431f8e0fe9a8126fdb675787bc79fb; yandex_login=gonzalofajardo; park_id=08e20910d81d42658d4334d3f6d10ac0; _ym_isad=2; yp=2079464530.multib.1#2083276938.udn.cDpHb256YWxvIEZhamFyZG8%3D#1768500383.yu.6009311931761677705; ymex=1771005983.oyu.6009311931761677705#2077037707.yrts.1761677707; _ym_visorc=w; _yasc=2mKcY8GygbjykCQel1V9urGg+aqTEZmAqVNXKfK1JjKMIohuQ3hRqw7ff6w9h+pemrcG; bh=EkEiR29vZ2xlIENocm9tZSI7dj0iMTQzIiwgIkNocm9taXVtIjt2PSIxNDMiLCAiTm90IEEoQnJhbmQiO3Y9IjI0IhoDeDg2Ig4xNDMuMC43NDk5LjE5MyoCPzA6CSJXaW5kb3dzIkIGMTkuMC4wSgI2NFJbIkdvb2dsZSBDaHJvbWUiO3Y9IjE0My4wLjc0OTkuMTkzIiwiQ2hyb21pdW0iO3Y9IjE0My4wLjc0OTkuMTkzIiwiTm90IEEoQnJhbmQiO3Y9IjI0LjAuMC4wImCjtp/LBmoe3Mrh/wiS2KGxA5/P4eoD+/rw5w3r//32D/vMzYcI"
    );
    
    private static final Random RANDOM = new Random();
    
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
     * Obtiene una cookie aleatoria del pool para rotación
     */
    protected String obtenerCookieAleatoria() {
        int index = RANDOM.nextInt(COOKIES_POOL.size());
        String cookie = COOKIES_POOL.get(index);
        log.debug("🍪 [BaseYangoApiService] Usando cookie #{} de {} disponibles", index + 1, COOKIES_POOL.size());
        return cookie;
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
        
        // Intentar con cada cookie del pool
        for (int i = 0; i < COOKIES_POOL.size(); i++) {
            String cookie = obtenerCookiePorIndice(i);
            HttpHeaders headers = headersFunc.apply(cookie);
            
            try {
                HttpEntity<T> request = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = getRestTemplate().exchange(url, method, request, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    if (i > 0) {
                        log.info("✅ [BaseYangoApiService] Retry exitoso con cookie #{} después de {} intentos fallidos", 
                            i + 1, i);
                    }
                    return response;
                }
                
                // Si no es exitoso pero tampoco es error crítico, retornar igual
                log.warn("⚠️ [BaseYangoApiService] Respuesta no exitosa con cookie #{}: {}", i + 1, response.getStatusCode());
                return response;
                
            } catch (HttpClientErrorException e) {
                ultimoError = e;
                int statusCode = e.getStatusCode().value();
                
                // Si es 401 (Unauthorized), 403 (Forbidden) o 429 (Too Many Requests), probar con otra cookie
                // 401 generalmente significa que la cookie expiró o no es válida
                if (statusCode == 401 || statusCode == 403 || statusCode == 429) {
                    String motivo = statusCode == 401 ? "Unauthorized (cookie expirada/inválida)" 
                                     : statusCode == 403 ? "Forbidden" 
                                     : "Too Many Requests";
                    log.warn("🔄 [BaseYangoApiService] Error {} con cookie #{} ({}), intentando con siguiente cookie...", 
                        statusCode, i + 1, motivo);
                    
                    // Esperar un poco antes de intentar con la siguiente cookie
                    try {
                        Thread.sleep(1000); // 1 segundo entre intentos
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue; // Intentar con siguiente cookie
                } else {
                    // Otro error HTTP, no es necesario probar con otras cookies
                    log.error("❌ [BaseYangoApiService] Error HTTP {} con cookie #{}: {}", 
                        statusCode, i + 1, e.getMessage());
                    throw e;
                }
                
            } catch (HttpServerErrorException e) {
                ultimoError = e;
                log.warn("🔄 [BaseYangoApiService] Error del servidor {} con cookie #{}, intentando con siguiente cookie...", 
                    e.getStatusCode().value(), i + 1);
                
                // Esperar un poco antes de intentar con la siguiente cookie
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                continue; // Intentar con siguiente cookie
                
            } catch (Exception e) {
                ultimoError = e;
                log.warn("🔄 [BaseYangoApiService] Error con cookie #{}: {}, intentando con siguiente cookie...", 
                    i + 1, e.getMessage());
                
                // Esperar un poco antes de intentar con la siguiente cookie
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                continue; // Intentar con siguiente cookie
            }
        }
        
        // Si llegamos aquí, todas las cookies fallaron
        log.error("❌ [BaseYangoApiService] Todas las {} cookies fallaron para URL: {}", COOKIES_POOL.size(), url);
        if (ultimoError != null) {
            throw ultimoError;
        }
        throw new RuntimeException("Todas las cookies fallaron y no hay más información del error");
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
}

