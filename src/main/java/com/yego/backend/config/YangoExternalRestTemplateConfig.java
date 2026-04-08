package com.yego.backend.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate con pool de conexiones hacia fleet.yango.com (API externa Yango).
 * Reutiliza TCP/TLS entre llamadas paralelas (suggestions + income x2 + goals).
 */
@Slf4j
@Configuration
public class YangoExternalRestTemplateConfig {

    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;

    @Bean(name = "yangoExternalRestTemplate")
    public RestTemplate yangoExternalRestTemplate() {
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSystemSocketFactory())
                .build();

        connectionManager = new PoolingHttpClientConnectionManager(reg);
        connectionManager.setMaxTotal(80);
        connectionManager.setDefaultMaxPerRoute(24);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(12))
                .setConnectionRequestTimeout(Timeout.ofSeconds(12))
                .setResponseTimeout(Timeout.ofSeconds(90))
                .build();

        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(Timeout.ofSeconds(45))
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(Duration.ofSeconds(12));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(12));

        log.info("[YangoExternalRestTemplate] Pool HTTP listo (maxTotal=80, maxPerRoute=24, keep-alive)");
        return new RestTemplate(factory);
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (Exception e) {
            log.warn("[YangoExternalRestTemplate] Cerrando HttpClient: {}", e.getMessage());
        }
        try {
            if (connectionManager != null) {
                connectionManager.close();
            }
        } catch (Exception e) {
            log.warn("[YangoExternalRestTemplate] Cerrando pool: {}", e.getMessage());
        }
    }
}
