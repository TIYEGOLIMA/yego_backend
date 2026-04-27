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

@Component
@Slf4j
public class ProxyUpdateScheduler {

    private final ProxyConfig proxyConfig;
    private final RestTemplate restTemplate;

    @Value("${yego.pro-ops.proxy.webshare-url:}")
    private String webshareUrl;

    public ProxyUpdateScheduler(ProxyConfig proxyConfig,
                                @Autowired(required = false) RestTemplate restTemplate) {
        this.proxyConfig = proxyConfig;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
    }

    @Scheduled(
        initialDelayString = "${yego.pro-ops.proxy.refresh-initial-delay-ms:5000}",
        fixedDelayString = "${yego.pro-ops.proxy.refresh-interval-ms:18000000}"
    )
    public void refreshProxyList() {
        if (!proxyConfig.isEnabled()) return;
        if (webshareUrl == null || webshareUrl.isBlank()) {
            log.warn("[ProxyUpdateScheduler] yego.pro-ops.proxy.webshare-url no configurada");
            return;
        }
        try {
            String body = restTemplate.getForObject(webshareUrl, String.class);
            if (body == null || body.isBlank()) {
                log.warn("[ProxyUpdateScheduler] respuesta vacía de Webshare");
                return;
            }
            List<String> lines = Arrays.stream(body.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
            proxyConfig.updateProxies(lines);
            log.info("[ProxyUpdateScheduler] lista refrescada ({} líneas)", lines.size());
        } catch (Exception e) {
            log.error("[ProxyUpdateScheduler] error descargando lista: {}", e.getMessage());
        }
    }
}
