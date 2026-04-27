package com.yego.backend.config.yego_pro_ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class YangoProxyRestTemplateConfig {

    private static final int MAX_TOTAL_CONNECTIONS = 50;
    private static final int MAX_PER_ROUTE = 5;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(60);
    private static final int MAX_PROXY_DELAY_MS = 80;

    private final ProxyConfig proxyConfig;
    private final AtomicInteger proxyIndex = new AtomicInteger(0);
    private final Random random = new Random();

    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;

    @Bean(name = "yangoProxyRestTemplate")
    public RestTemplate yangoProxyRestTemplate() {
        List<String> proxies = proxyConfig.getProxies();
        if (!proxyConfig.isEnabled() || proxies == null || proxies.isEmpty()) {
            log.warn("[YangoProxy] rotación deshabilitada; usando conexión directa");
            return new RestTemplate();
        }
        log.info("[YangoProxy] rotación habilitada con {} proxies", proxies.size());
        this.httpClient = createHttpClientWithProxy();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setConnectionRequestTimeout(CONNECT_TIMEOUT);
        return new RestTemplate(factory);
    }

    private CloseableHttpClient createHttpClientWithProxy() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", SSLConnectionSocketFactory.getSystemSocketFactory())
            .build();

        this.connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_PER_ROUTE);

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(30))
            .setConnectionRequestTimeout(Timeout.ofSeconds(30))
            .setResponseTimeout(RESPONSE_TIMEOUT)
            .build();

        DynamicCredentialsProvider credentialsProvider = new DynamicCredentialsProvider();
        RotatingProxyRoutePlanner routePlanner = new RotatingProxyRoutePlanner(this, credentialsProvider);

        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setRoutePlanner(routePlanner)
            .setDefaultCredentialsProvider(credentialsProvider)
            .build();
    }

    ProxyInfo getNextProxyInfo() {
        List<String> proxies = proxyConfig.getProxies();
        if (proxies == null || proxies.isEmpty()) return null;

        int index = Math.abs(proxyIndex.getAndIncrement()) % proxies.size();
        try {
            return parseProxy(proxies.get(index));
        } catch (Exception e) {
            log.error("[YangoProxy] error parseando proxy índice {}: {}", index, e.getMessage());
            if (proxies.size() > 1) {
                try {
                    return parseProxy(proxies.get((index + 1) % proxies.size()));
                } catch (Exception ignored) {}
            }
            return null;
        }
    }

    private ProxyInfo parseProxy(String proxyString) {
        String[] parts = proxyString.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Formato de proxy inválido (IP:PORT:USER:PASS)");
        }
        return new ProxyInfo(parts[0], Integer.parseInt(parts[1]), parts[2], parts[3]);
    }

    record ProxyInfo(String host, int port, String username, String password) {}

    private static final class DynamicCredentialsProvider extends BasicCredentialsProvider {
        @Override
        public Credentials getCredentials(AuthScope authscope, HttpContext context) {
            if (authscope.getHost() != null && context != null) {
                ProxyInfo proxyInfo = (ProxyInfo) context.getAttribute("current.proxy");
                if (proxyInfo != null
                    && authscope.getHost().equals(proxyInfo.host())
                    && authscope.getPort() == proxyInfo.port()) {
                    return new UsernamePasswordCredentials(proxyInfo.username(), proxyInfo.password().toCharArray());
                }
            }
            return super.getCredentials(authscope, context);
        }
    }

    private static final class RotatingProxyRoutePlanner implements HttpRoutePlanner {
        private final YangoProxyRestTemplateConfig config;
        private final DynamicCredentialsProvider credentialsProvider;

        RotatingProxyRoutePlanner(YangoProxyRestTemplateConfig config, DynamicCredentialsProvider credentialsProvider) {
            this.config = config;
            this.credentialsProvider = credentialsProvider;
        }

        @Override
        public HttpRoute determineRoute(HttpHost target, HttpContext context) throws HttpException {
            HttpHost normalizedTarget = normalizeTarget(target);
            ProxyInfo proxyInfo = config.getNextProxyInfo();
            if (proxyInfo == null) return new HttpRoute(normalizedTarget);

            context.setAttribute("current.proxy", proxyInfo);
            credentialsProvider.setCredentials(
                new AuthScope(proxyInfo.host(), proxyInfo.port()),
                new UsernamePasswordCredentials(proxyInfo.username(), proxyInfo.password().toCharArray())
            );

            try {
                int delayMs = config.random.nextInt(MAX_PROXY_DELAY_MS);
                if (delayMs > 0) Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            HttpHost proxy = new HttpHost("http", proxyInfo.host(), proxyInfo.port());
            log.debug("[YangoProxy] route {}:{} via proxy {}:{}",
                normalizedTarget.getHostName(), normalizedTarget.getPort(),
                proxyInfo.host(), proxyInfo.port());
            return new HttpRoute(normalizedTarget, proxy);
        }

        private HttpHost normalizeTarget(HttpHost target) {
            if (target.getPort() >= 0) return target;
            String scheme = target.getSchemeName();
            int defaultPort = "https".equalsIgnoreCase(scheme) ? 443 : 80;
            return new HttpHost(scheme, target.getHostName(), defaultPort);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (connectionManager != null) {
                connectionManager.close();
                connectionManager = null;
            }
            if (httpClient != null) {
                httpClient.close();
                httpClient = null;
            }
            log.info("[YangoProxy] conexiones HTTP cerradas");
        } catch (Exception e) {
            log.error("[YangoProxy] error cerrando conexiones: {}", e.getMessage(), e);
        }
    }
}
