package com.yego.backend.service.yego_api_externo;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cliente HTTP Yango para yego_api_externo: pool de cookies y retry propios (no comparte BaseYangoApiService).
 */
@Slf4j
@Component
public class YangoClient {

    static final String DEFAULT_PARK_ID = "64085dd85e124e2c808806f70d527ea8";
    static final String SUGGESTIONS_LIST_URL =
            "https://fleet.yango.com/api/fleet/contractor-profiles-manager/v1/suggestions/list";
    static final String GOALS_URL_TEMPLATE =
            "https://fleet.yango.com/api/fleet/v1/subvention-view/v1/goals?driver_profile_id=%s";

    private static final int RETRY_401_DELAY_MS = 75;

    /** Soporte (1), luego cookies de respaldo; el cliente elige al azar entre las no marcadas inválidas. */
    private static final List<String> COOKIES_POOL = Arrays.asList(
            "yandexuid=3749697061773160569; yashr=4439789851773160569; receive-cookie-deprecation=1; _ym_uid=177316057294224056; _ym_d=1773160572; gdpr=0; Session_id=3:1774364989.5.0.1774364989907:WbD9Jg:2ba1.1.2:1|2221285626.0.2.0:3.3:1774364989|60:11796558.594160.n_Jmlkg1gEuCZt_1XvVWkN4dw8c; sessar=1.1719225.CiBVRVJOGbtkgeSto1PnCayxyvxUggJdZy4wgPsQQWcnsA.2wfis8ZmdUDtbnJ2950KmXXfVO-vBnzfZpSYDEBg9GI; sessionid2=3:1774364989.5.0.1774364989907:WbD9Jg:2ba1.1.2:1|2221285626.0.2.0:3.3:1774364989|60:11796558.594160.fakesign0000000000000000000; L=XDBnAWBleFpjWXRwR1dCdndTf2BGemB6JgIzVhRDLDAHKjk=.1774364989.1789215.35875.8178e2f5138ad768bc37ca50165d71c0; yandex_login=soporteyego; park_id=08e20910d81d42658d4334d3f6d10ac0; yuidss=3749697061773160569; _ym_isad=1; i=kplR60Mbv53VkwobjZR/PxHbH1JYt/UC9D43ZYp1bVYN/8ZCbb5OVX+bbJ0NTu9BTKvH22HDPyjwMegPXZIPMs1InGE=; yp=2089724989.udn.cDpzb3BvcnRleWVnbw%3D%3D#1778333796.yu.3749697061773160569; ymex=1780839396.oyu.3749697061773160569#2093466452.yrts.1778106452; _ym_visorc=b; _yasc=xgTG80qDPHCLlaz3fQZ/hX0uwhO3yDulSKpmPR0Er27f7A68vRd+Xr2FytzEoenmecQItnt6.MTc3ODI0OTk0NDgxNw==; bh=EkAiR29vZ2xlIENocm9tZSI7dj0iMTQ3IiwgIk5vdC5BL0JyYW5kIjt2PSI4IiwgIkNocm9taXVtIjt2PSIxNDciGgN4ODYiDjE0Ny4wLjc3MjcuMTM3KgI/MDoHIkxpbnV4IkoCNjRSWiJHb29nbGUgQ2hyb21lIjt2PSIxNDcuMC43NzI3LjEzNyIsIk5vdC5BL0JyYW5kIjt2PSI4LjAuMC4wIiwiQ2hyb21pdW0iO3Y9IjE0Ny4wLjc3MjcuMTM3ImCW6ffPBmoZ3MrpiA7yrLelC/v68OcN6//99g+bh8+HCA==",
            "yashr=299832191758141387; receive-cookie-deprecation=1; gdpr=0; _ym_uid=175814138831171998; _ym_d=1758141396; yandexuid=9201743261758137514; yuidss=9201743261758137514; yandex_login=Jhajaira.ochoa; L=ZxtXZnxqBmR/SAVhdVZyXwJfaA9cBV1cLlAoE1ddHRAZOQxQLTM=.1759148388.1235855.353634.d47e5d7bafd679e1c83d4f42c6e23cd9; Session_id=3:1764107903.5.0.1758141420632:WbD9Jg:3a2a.1.2:1|2015824474.1006968.2.0:3.2:1006968.3:1759148388|60:11433100.181615.QwZv5z-R92wHzh4j43-daY0-K7g; sessar=1.1396519.CiDPtjUqckvAWEzdml3-Xvlj9hjrxSX6tJqOaxrdeZn5IA.jv1_Nvn5vO56j4mwzyFG1Wg8pHX_1BWCf-tAOHo6bQk; sessionid2=3:1764107903.5.0.1758141420632:WbD9Jg:3a2a.1.2:1|2015824474.1006968.2.0:3.2:1006968.3:1759148388|60:11433100.181615.fakesign0000000000000000000; i=XLXWMoCXAOgX6hzpx/AmT+HOGGwAwQhiTRKOzxl2tkMXu90DChcwoTT5z8qvZlmQyhkwZXurYZsuUa9AHb5foPXF2Rc=; _ym_isad=2; yp=2074508388.udn.cDpKaGFqYWlyYS5vY2hvYQ%3D%3D#1764940883.yu.9201743261758137514; ymex=1767446483.oyu.9201743261758137514#2073501389.yrts.1758141389; _ym_visorc=b; bh=Ej8iQ2hyb21pdW0iO3Y9IjE0MiIsIkdvb2dsZSBDaHJvbWUiO3Y9IjE0MiIsIk5vdF9BIEJyYW5kIjt2PSI5OSIaA3g4NiIOMTQyLjAuNzQ0NC4xNzYqAj8wOgdXaW5kb3dzQgYxMC4wLjBKAjY0UlsiQ2hyb21pdW0iO3Y9IjE0Mi4wLjc0NDQuMTc2IiwiR29vZ2xlIENocm9tZSI7dj0iMTQyLjAuNzQ0NC4xNzYiLCJOb3RfQSBCcmFuZCI7dj0iOTkuMC4wLjAiYPzux8kGah7cyuH/CJLYobEDn8/h6gP7+vDnDev//fYP+JzMhwg=; _yasc=fAS4rNTIoRKZq+JMg4Gaj9eUc5ZA62kxyJug7jaq8e08yG17i2jy7kvgtoikFrhpuyUc",
            "i=nOvRJ2K/cZwWkUf8kHuUrHWLCzY44glVytXeWCF8UjS0SnNco2YVTIDg8AGF5VTgKfZpWwEgwj6jgfVAZT4CTM4GaRw=; yandexuid=2622840871772483002; yashr=7273643541772483002; yuidss=2622840871772483002; ymex=2087843007.yrts.1772483007; gdpr=0; _ym_uid=1772483007632502910; _ym_d=1772483008; yandex_login=giomarortega; Session_id=3:1774442763.5.0.1772483086636:WbD9Jg:6f7d.1.2:1|2223153146.1959677.2.0:3.2:1959677.3:1774442763|60:11798385.480979.JYPTGmu58GOQPKLBMu9rRkEHrr4; sessar=1.1719225.CiAOZ6ib9gwbFlcv0qu857wUi0jhWNkeP48249Vo8T9AKg.RQziCqX7K3iUtu4q9olsJ5CslGkIPLX95kQrckocJho; sessionid2=3:1774442763.5.0.1772483086636:WbD9Jg:6f7d.1.2:1|2223153146.1959677.2.0:3.2:1959677.3:1774442763|60:11798385.480979.fakesign0000000000000000000; yp=2089802763.udn.cDpnaW9tYXJvcnRlZ2E%3D; L=QxhlAnNeXUZAYQIAVWxjZ0Bte3FZBHNzVV4oASoTBgYDNikF.1774442763.1791208.32.2e81a1506afacf8869b3eed098c3419d; _ym_isad=2; _yasc=WcDTEm5qOHIi/l6OfbjsX7k/M6Sbr40FBY+s/spoWL+aafhVuc0bWhrv6Dr1l/p99yMPNIkZ.MTc3ODI0OTM1MTA2Mw==; bh=EkEiTm90KEE6QnJhbmQiO3Y9IjgiLCAiQ2hyb21pdW0iO3Y9IjE0NCIsICJNaWNyb3NvZnQgRWRnZSI7dj0iMTQ0IhoDeDg2Ig4xNDQuMC4zNzE5LjExNSoCPzEyB05leHVzIDU6CSJBbmRyb2lkIkIDNi4wSgI2NFJbIk5vdChBOkJyYW5kIjt2PSI4LjAuMC4wIiwiQ2hyb21pdW0iO3Y9IjE0NC4wLjc1NTkuMTMzIiwiTWljcm9zb2Z0IEVkZ2UiO3Y9IjE0NC4wLjM3MTkuMTE1ImCl6vfPBmom3Mql7AbPn4yfBaynvLsFoJ3s6wP8ua//B9/9+9wH5bXNhwi/gwM=",
            "yandexuid=6009311931761677705; yashr=4567181961761677705; yuidss=6009311931761677705; receive-cookie-deprecation=1; gdpr=0; _ym_uid=1761677706290870939; _ym_d=1761677707; i=WPv45DbbiaiQTfOesurzPwPDcYTOSOoBsoiMqnCbM8UNdwnBPqcEVeWPbr+/nREEJsBHtNGb/FFdfO9HLKe33wC900U=; Session_id=3:1767916938.5.0.1761677775316:WbD9Jg:71df.1.2:1|1782860170.0.2.0:3.3:1761677775|2220343194.-1.0.0:3.2:2426755.3:1764104530|60:11590429.497800.3oxV2BfdArdTZgb0iJuNpsl7pTQ; sessar=1.1504434.CiBoQiEtUqB8Clq12Z20uRTAKOBx8rNaYp3ayAviftuOlQ.HmHoWlhWBAmESc7SsPOk0_fwoYScX1OCMb7N5WOch3w; sessionid2=3:1767916938.5.0.1761677775316:WbD9Jg:71df.1.2:1|1782860170.0.2.0:3.3:1761677775|2220343194.-1.0.0:3.2:2426755.3:1764104530|60:11590429.497800.fakesign0000000000000000000; L=cwNkZHx2bUdLfgZ1eX1WBUJKR0J7dVdUATclGQI2VgomGwMkLic=.1767916938.1586619.346423.d1431f8e0fe9a8126fdb675787bc79fb; yandex_login=gonzalofajardo; park_id=08e20910d81d42658d4334d3f6d10ac0; _ym_isad=2; yp=2079464530.multib.1#2083276938.udn.cDpHb256YWxvIEZhamFyZG8%3D#1768500383.yu.6009311931761677705; ymex=1771005983.oyu.6009311931761677705#2077037707.yrts.1761677707; _ym_visorc=w; _yasc=2mKcY8GygbjykCQel1V9urGg+aqTEZmAqVNXKfK1JjKMIohuQ3hRqw7ff6w9h+pemrcG; bh=EkEiR29vZ2xlIENocm9tZSI7dj0iMTQzIiwgIkNocm9taXVtIjt2PSIxNDMiLCAiTm90IEEoQnJhbmQiO3Y9IjI0IhoDeDg2Ig4xNDMuMC43NDk5LjE5MyoCPzA6CSJXaW5kb3dzIkIGMTkuMC4wSgI2NFJbIkdvb2dsZSBDaHJvbWUiO3Y9IjE0My4wLjc0OTkuMTkzIiwiQ2hyb21pdW0iO3Y9IjE0My4wLjc0OTkuMTkzIiwiTm90IEEoQnJhbmQiO3Y9IjI0LjAuMC4wImCjtp/LBmoe3Mrh/wiS2KGxA5/P4eoD+/rw5w3r//32D/vMzYcI"
    );

    private static final Random RANDOM = new Random();
    private static final Set<Integer> COOKIE_INDICES_INVALIDOS = ConcurrentHashMap.newKeySet();

    private final RestTemplate restTemplate;

    public YangoClient(@Qualifier("yangoExternalRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("[YangoClient] Inicializado cookies={} RestTemplate=pool(Yango external)", COOKIES_POOL.size());
    }

    private static void marcarCookieInvalida(int index) {
        if (index >= 0 && index < COOKIES_POOL.size()) {
            COOKIE_INDICES_INVALIDOS.add(index);
            log.info("[YangoClient] Cookie #{} marcada inválida", index + 1);
        }
    }

    private static int obtenerIndiceCookieValida() {
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

    private static HttpHeaders headersSuggestions(String cookie, String parkId) {
        String pid = parkId != null && !parkId.isBlank() ? parkId : DEFAULT_PARK_ID;
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

    private static HttpHeaders headersFleetJson(String cookie, String parkId) {
        String pid = parkId != null && !parkId.isBlank() ? parkId : DEFAULT_PARK_ID;
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

    /**
     * POST suggestions/list — conexión directa (sin proxy) para menor latencia.
     */
    public ResponseEntity<String> postSuggestions(String bodyJson, String parkId) throws Exception {
        return exchangeDirect(SUGGESTIONS_LIST_URL, HttpMethod.POST, bodyJson, cookie -> headersSuggestions(cookie, parkId));
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
        for (int attempt = 0; attempt < COOKIES_POOL.size(); attempt++) {
            int i = obtenerIndiceCookieValida();
            if (i < 0) { COOKIE_INDICES_INVALIDOS.clear(); break; }
            String cookie = COOKIES_POOL.get(i);
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
                    marcarCookieInvalida(i);
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
        COOKIE_INDICES_INVALIDOS.clear();
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
