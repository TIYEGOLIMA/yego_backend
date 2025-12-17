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
                log.warn("⚠️ [ProxyConfig] No se encontró el archivo de proxies: {}", proxyFile);
                enabled = false;
                return;
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                
                proxies = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .filter(line -> line.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+:.+:.+$"))
                    .collect(Collectors.toList());
                
                log.info("✅ [ProxyConfig] {} proxies cargados desde {}", proxies.size(), proxyFile);
                
                if (proxies.isEmpty()) {
                    log.warn("⚠️ [ProxyConfig] No se encontraron proxies válidos en el archivo");
                    enabled = false;
                }
            }
        } catch (Exception e) {
            log.error("❌ [ProxyConfig] Error cargando proxies desde archivo: {}", e.getMessage(), e);
            enabled = false;
            proxies = new ArrayList<>();
        }
    }
}
