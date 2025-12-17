package com.yego.backend.config.yego_pro_ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuración de RestTemplate con soporte para rotación de proxies para la API de Yango
 * Usa Apache HttpClient que maneja mejor la autenticación de proxies
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class YangoProxyRestTemplateConfig {
    
    private final ProxyConfig proxyConfig;
    private final AtomicInteger proxyIndex = new AtomicInteger(0);
    
    
    @Bean(name = "yangoProxyRestTemplate")
    @Primary
    public RestTemplate yangoProxyRestTemplate() {
        if (proxyConfig.isEnabled() && proxyConfig.getProxies() != null && !proxyConfig.getProxies().isEmpty()) {
            CloseableHttpClient httpClient = createHttpClientWithProxy();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(java.time.Duration.ofSeconds(30));
            factory.setConnectionRequestTimeout(java.time.Duration.ofSeconds(30));
            return new RestTemplate(factory);
        }
        return new RestTemplate();
    }
    
    private CloseableHttpClient createHttpClientWithProxy() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", SSLConnectionSocketFactory.getSystemSocketFactory())
            .build();
        
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(20);
        
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(30))
            .setConnectionRequestTimeout(Timeout.ofSeconds(30))
            .setResponseTimeout(Timeout.ofSeconds(60))
            .build();
        
        DynamicCredentialsProvider credentialsProvider = new DynamicCredentialsProvider(this);
        RotatingProxyRoutePlanner routePlanner = new RotatingProxyRoutePlanner(this, credentialsProvider);
        
        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setRoutePlanner(routePlanner)
            .setDefaultCredentialsProvider(credentialsProvider)
            .build();
    }
    
    ProxyInfo getNextProxyInfo() {
        var proxies = proxyConfig.getProxies();
        if (proxies == null || proxies.isEmpty()) {
            return null;
        }
        
        int index = proxyIndex.getAndIncrement() % proxies.size();
        String proxyString = proxies.get(index);
        
        try {
            return parseProxy(proxyString);
        } catch (Exception e) {
            log.error("❌ [YangoProxyRestTemplateConfig] Error parseando proxy: {}", e.getMessage());
            return null;
        }
    }
    
    private ProxyInfo parseProxy(String proxyString) {
        String[] parts = proxyString.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Formato de proxy inválido. Debe ser IP:PORT:USERNAME:PASSWORD");
        }
        return new ProxyInfo(parts[0], Integer.parseInt(parts[1]), parts[2], parts[3]);
    }
    
    static class ProxyInfo {
        String host;
        int port;
        String username;
        String password;
        
        ProxyInfo(String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }
    }
    
    /**
     * CredentialsProvider dinámico que obtiene credenciales del proxy actual
     */
    private static class DynamicCredentialsProvider extends BasicCredentialsProvider {
        private final YangoProxyRestTemplateConfig config;
        
        DynamicCredentialsProvider(YangoProxyRestTemplateConfig config) {
            this.config = config;
        }
        
        @Override
        public org.apache.hc.client5.http.auth.Credentials getCredentials(AuthScope authscope, HttpContext context) {
            if (authscope.getHost() != null && context != null) {
                ProxyInfo proxyInfo = (ProxyInfo) context.getAttribute("current.proxy");
                if (proxyInfo != null && 
                    authscope.getHost().equals(proxyInfo.host) && 
                    authscope.getPort() == proxyInfo.port) {
                    return new UsernamePasswordCredentials(proxyInfo.username, proxyInfo.password.toCharArray());
                }
            }
            return super.getCredentials(authscope, context);
        }
    }
    
    /**
     * RoutePlanner personalizado que rota entre múltiples proxies
     */
    private static class RotatingProxyRoutePlanner implements HttpRoutePlanner {
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
            
            if (proxyInfo != null) {
                context.setAttribute("current.proxy", proxyInfo);
                credentialsProvider.setCredentials(
                    new AuthScope(proxyInfo.host, proxyInfo.port),
                    new UsernamePasswordCredentials(proxyInfo.username, proxyInfo.password.toCharArray())
                );
                
                HttpHost proxy = new HttpHost("http", proxyInfo.host, proxyInfo.port);
                log.debug("🔄 [RotatingProxyRoutePlanner] Usando proxy: {}:{} para target: {}:{}", 
                    proxyInfo.host, proxyInfo.port, normalizedTarget.getHostName(), normalizedTarget.getPort());
                
                return new HttpRoute(normalizedTarget, proxy);
            }
            
            return new HttpRoute(normalizedTarget);
        }
        
        /**
         * Normaliza el HttpHost target estableciendo el puerto por defecto si es -1
         */
        private HttpHost normalizeTarget(HttpHost target) {
            if (target.getPort() >= 0) {
                return target;
            }
            
            String scheme = target.getSchemeName();
            int defaultPort = "https".equalsIgnoreCase(scheme) ? 443 : 80;
            
            return new HttpHost(scheme, target.getHostName(), defaultPort);
        }
    }
}
