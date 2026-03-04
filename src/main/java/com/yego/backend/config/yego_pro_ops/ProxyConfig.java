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
    
    @Value("${yego.pro-ops.proxy.enabled:false}")
    private boolean enabled = false;
    
    @Value("${yego.pro-ops.proxy.file:proxies.txt}")
    private String proxyFile = "proxies.txt";
    
    private List<String> proxies = new ArrayList<>();

    /** Formato esperado por línea: IP:PUERTO:USUARIO:CONTRASEÑA */
    private static final String PROXY_LINE_REGEX = "^\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+:.+:.+$";
    
    @PostConstruct
    public void init() {
        if (enabled) {
            loadProxiesFromFile();
        } else {
            log.info("ℹ️ [ProxyConfig] Rotación de proxies deshabilitada");
        }
    }
    
    private void loadProxiesFromFile() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(proxyFile);
            if (inputStream == null) {
                log.info("ℹ️ [ProxyConfig] No hay archivo {}; la lista se cargará desde Webshare (scheduler)", proxyFile);
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
                    log.info("ℹ️ [ProxyConfig] Archivo {} vacío o sin líneas válidas; la lista se cargará desde Webshare", proxyFile);
                } else {
                    log.info("✅ [ProxyConfig] {} proxies cargados desde {} (opcional; el scheduler actualizará desde Webshare)", proxies.size(), proxyFile);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [ProxyConfig] No se pudo leer {}: {}. La lista se cargará desde Webshare.", proxyFile, e.getMessage());
        }
    }

    /**
     * Actualiza la lista de proxies en memoria (thread-safe).
     * Usado por el scheduler que descarga la lista desde Webshare cada 5 horas.
     *
     * @param newProxies lista en formato IP:PORT:USER:PASSWORD por línea
     */
    public synchronized void updateProxies(List<String> newProxies) {
        if (newProxies == null || newProxies.isEmpty()) {
            log.warn("⚠️ [ProxyConfig] updateProxies: lista vacía, no se actualiza");
            return;
        }
        List<String> valid = newProxies.stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .filter(line -> line.matches(PROXY_LINE_REGEX))
                .collect(Collectors.toList());
        this.proxies = new ArrayList<>(valid);
        log.info("✅ [ProxyConfig] Lista de proxies actualizada: {} proxies válidos", this.proxies.size());
    }
}
