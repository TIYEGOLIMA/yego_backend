package com.yego.backend.config;

import com.yego.backend.entity.yego_api_externo.entities.YangoApiLog;
import com.yego.backend.repository.yego_api_externo.YangoApiLogRepository;
import com.yego.backend.service.yego_principal.FilteredWebSocketService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class YangoApiLogFilter implements Filter {

    private final YangoApiLogRepository yangoApiLogRepository;
    private final FilteredWebSocketService filteredWebSocketService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        String uri = httpReq.getRequestURI();

        if (!uri.startsWith("/api/yango-external/")) {
            chain.doFilter(request, response);
            return;
        }

        // No registrar ni notificar el propio endpoint de consulta de logs (evita ruido y bucles)
        if (uri.endsWith("/logs") || uri.contains("/yango-external/logs")) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedReq = new ContentCachingRequestWrapper(httpReq);
        long start = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedReq, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            HttpServletResponse httpRes = (HttpServletResponse) response;

            String body = new String(wrappedReq.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (body.length() > 2000) body = body.substring(0, 2000);

            try {
                YangoApiLog saved = yangoApiLogRepository.save(YangoApiLog.builder()
                        .endpoint(uri)
                        .method(httpReq.getMethod())
                        .ipAddress(resolveIp(httpReq))
                        .requestBody(body.isEmpty() ? null : body)
                        .statusCode(httpRes.getStatus())
                        .responseTimeMs(elapsed)
                        .userAgent(truncate(httpReq.getHeader("User-Agent"), 500))
                        .build());

                Map<String, Object> ws = new HashMap<>();
                ws.put("type", "YANGO_API_LOG_UPDATED");
                ws.put("logId", saved.getId());
                ws.put("endpoint", uri);
                ws.put("timestamp", LocalDateTime.now().toString());
                filteredWebSocketService.convertAndSend("/topic/system", ws);
            } catch (Exception e) {
                log.error("[YangoApiLogFilter] Error guardando log: {}", e.getMessage());
            }
        }
    }

    private static String resolveIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip.trim();
        return req.getRemoteAddr();
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
