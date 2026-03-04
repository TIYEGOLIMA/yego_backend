package com.yego.backend.scheduler.yego_pro_ops;

import com.yego.backend.config.yego_pro_ops.ProxyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduler que actualiza la lista de proxies desde Webshare cada 5 horas.
 * La URL descarga un listado en formato IP:PORT:USER:PASSWORD (una línea por proxy).
 *
 * @see ProxyConfig#updateProxies(List)
 */
@Component
@Slf4j
public class ProxyUpdateScheduler {

    private final ProxyConfig proxyConfig;
    private final RestTemplate restTemplate;

    @Value("${yego.pro-ops.proxy.webshare-url:}")
    private String webshareUrl;

    public ProxyUpdateScheduler(ProxyConfig proxyConfig, @Autowired(required = false) RestTemplate restTemplate) {
        this.proxyConfig = proxyConfig;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
    }

    /**
     * Actualiza la lista de proxies desde la URL de Webshare.
     * Primera ejecución: pocos segundos después del arranque (no hace falta proxies.txt).
     * Siguientes: cada 5 horas.
     */
    @Scheduled(initialDelayString = "${yego.pro-ops.proxy.refresh-initial-delay-ms:5000}", fixedDelayString = "${yego.pro-ops.proxy.refresh-interval-ms:18000000}")
    public void refreshProxyList() {
        if (!proxyConfig.isEnabled()) {
            log.debug("[ProxyUpdateScheduler] Proxy deshabilitado, no se actualiza la lista");
            return;
        }
        if (webshareUrl == null || webshareUrl.isBlank()) {
            log.warn("⚠️ [ProxyUpdateScheduler] yego.pro-ops.proxy.webshare-url no configurada, no se actualiza la lista");
            return;
        }
        try {
            String body = restTemplate.getForObject(webshareUrl, String.class);
            if (body == null || body.isBlank()) {
                log.warn("⚠️ [ProxyUpdateScheduler] Respuesta vacía de Webshare");
                return;
            }
            List<String> lines = Arrays.stream(body.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
            proxyConfig.updateProxies(lines);
            log.info("🔄 [ProxyUpdateScheduler] Lista de proxies refrescada desde Webshare ({} líneas)", lines.size());
        } catch (Exception e) {
            log.error("❌ [ProxyUpdateScheduler] Error descargando lista de proxies desde Webshare: {}", e.getMessage());
        }
    }
}
