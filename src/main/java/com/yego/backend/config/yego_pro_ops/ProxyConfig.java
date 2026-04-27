package com.yego.backend.config.yego_pro_ops;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
@Configuration
public class ProxyConfig {

    private static final String PROXY_LINE_REGEX = "^\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+:.+:.+$";

    @Value("${yego.pro-ops.proxy.enabled:false}")
    private boolean enabled = false;

    @Value("${yego.pro-ops.proxy.file:proxies.txt}")
    private String proxyFile = "proxies.txt";

    private List<String> proxies = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("[ProxyConfig] rotación de proxies deshabilitada");
            return;
        }
        loadProxiesFromFile();
    }

    private void loadProxiesFromFile() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(proxyFile);
            if (inputStream == null) {
                log.info("[ProxyConfig] sin archivo {}; se cargará desde Webshare (scheduler)", proxyFile);
                return;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                proxies = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .filter(line -> line.matches(PROXY_LINE_REGEX))
                    .collect(Collectors.toList());
                if (proxies.isEmpty()) {
                    log.info("[ProxyConfig] archivo {} vacío; se cargará desde Webshare", proxyFile);
                } else {
                    log.info("[ProxyConfig] {} proxies cargados desde {}", proxies.size(), proxyFile);
                }
            }
        } catch (Exception e) {
            log.warn("[ProxyConfig] no se pudo leer {}: {}", proxyFile, e.getMessage());
        }
    }

    public synchronized void updateProxies(List<String> newProxies) {
        if (newProxies == null || newProxies.isEmpty()) {
            log.warn("[ProxyConfig] updateProxies recibió lista vacía; no se actualiza");
            return;
        }
        this.proxies = newProxies.stream()
            .map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .filter(line -> line.matches(PROXY_LINE_REGEX))
            .collect(Collectors.toCollection(ArrayList::new));
        log.info("[ProxyConfig] lista actualizada: {} proxies válidos", this.proxies.size());
    }
}
