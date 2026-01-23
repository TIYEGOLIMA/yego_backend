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
import java.util.Random;
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
    private final Random random = new Random();
    
    // Guardar referencias para cerrar al hacer shutdown
    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;
    
    
    @Bean(name = "yangoProxyRestTemplate")
    public RestTemplate yangoProxyRestTemplate() {
        if (proxyConfig.isEnabled() && proxyConfig.getProxies() != null && !proxyConfig.getProxies().isEmpty()) {
            log.info("✅ [YangoProxyRestTemplateConfig] Rotación de proxies HABILITADA - {} proxies disponibles", 
                proxyConfig.getProxies().size());
            this.httpClient = createHttpClientWithProxy();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(java.time.Duration.ofSeconds(30));
            factory.setConnectionRequestTimeout(java.time.Duration.ofSeconds(30));
            return new RestTemplate(factory);
        } else {
            log.warn("⚠️ [YangoProxyRestTemplateConfig] Rotación de proxies DESHABILITADA - usando conexión directa");
            return new RestTemplate();
        }
    }
    
    private CloseableHttpClient createHttpClientWithProxy() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", SSLConnectionSocketFactory.getSystemSocketFactory())
            .build();
        
        this.connectionManager = 
            new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // Reducir conexiones para evitar saturar Nginx (768 worker_connections)
        // Máximo 50 conexiones totales, 5 por ruta para distribuir mejor
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(5);
        
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
    
    /**
     * Obtiene el siguiente proxy de forma rotativa
     * Usa un índice atómico para garantizar distribución uniforme entre todos los proxies
     */
    ProxyInfo getNextProxyInfo() {
        var proxies = proxyConfig.getProxies();
        if (proxies == null || proxies.isEmpty()) {
            log.warn("⚠️ [YangoProxyRestTemplateConfig] No hay proxies disponibles");
            return null;
        }
        
        // Rotación circular: cada request usa un proxy diferente
        int index = Math.abs(proxyIndex.getAndIncrement()) % proxies.size();
        String proxyString = proxies.get(index);
        
        try {
            ProxyInfo proxyInfo = parseProxy(proxyString);
            // Log solo cuando cambia el proxy para no saturar (cada N requests)
            if (index % 10 == 0 || log.isDebugEnabled()) {
            log.debug("🔄 [YangoProxyRestTemplateConfig] Seleccionado proxy {}/{}: {}:{}", 
                index + 1, proxies.size(), proxyInfo.host, proxyInfo.port);
            }
            return proxyInfo;
        } catch (Exception e) {
            log.error("❌ [YangoProxyRestTemplateConfig] Error parseando proxy en índice {}: {}", index, e.getMessage());
            // Si falla, intentar con el siguiente proxy
            if (proxies.size() > 1) {
                int nextIndex = (index + 1) % proxies.size();
                try {
                    return parseProxy(proxies.get(nextIndex));
                } catch (Exception e2) {
                    log.error("❌ [YangoProxyRestTemplateConfig] Error también con proxy siguiente: {}", e2.getMessage());
                }
            }
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
                
                // Pequeño delay aleatorio (0-500ms) para distribuir mejor las requests entre proxies
                // Esto ayuda a evitar que múltiples requests simultáneas usen el mismo proxy
                try {
                    int delayMs = config.random.nextInt(500); // 0-500ms aleatorio
                    if (delayMs > 0) {
                        Thread.sleep(delayMs);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                HttpHost proxy = new HttpHost("http", proxyInfo.host, proxyInfo.port);
                // Log en nivel info para identificar qué proxy se está usando cuando hay errores
                log.info("🔄 [RotatingProxyRoutePlanner] Usando proxy {}:{} (usuario: {}) para target {}:{}", 
                        proxyInfo.host, proxyInfo.port, proxyInfo.username, normalizedTarget.getHostName(), normalizedTarget.getPort());
                
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
    
    /**
     * Cierra todas las conexiones HTTP al hacer shutdown
     * Se ejecuta automáticamente cuando Spring destruye el bean
     */
    @jakarta.annotation.PreDestroy
    public void cleanup() {
        log.info("🧹 [YangoProxyRestTemplateConfig] Cerrando conexiones HTTP...");
        try {
            if (connectionManager != null) {
                log.info("🔄 [YangoProxyRestTemplateConfig] Cerrando ConnectionManager y todas las conexiones activas...");
                connectionManager.close();
                connectionManager = null;
                log.info("✅ [YangoProxyRestTemplateConfig] ConnectionManager cerrado");
            }
            
            if (httpClient != null) {
                log.info("🔄 [YangoProxyRestTemplateConfig] Cerrando HttpClient...");
                httpClient.close();
                httpClient = null;
                log.info("✅ [YangoProxyRestTemplateConfig] HttpClient cerrado");
            }
        } catch (Exception e) {
            log.error("❌ [YangoProxyRestTemplateConfig] Error cerrando conexiones HTTP: {}", e.getMessage(), e);
        }
        log.info("✅ [YangoProxyRestTemplateConfig] Limpieza de conexiones HTTP completada");
    }
}
