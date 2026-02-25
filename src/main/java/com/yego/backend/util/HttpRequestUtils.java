package com.yego.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Utilidades para extraer información de la petición HTTP.
 */
@Slf4j
public final class HttpRequestUtils {

    private HttpRequestUtils() {
    }

    /**
     * Obtiene la IP real del cliente, considerando proxies (X-Forwarded-For, X-Real-IP).
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String xRealIp = request.getHeader("X-Real-IP");
        String remoteAddr = request.getRemoteAddr();

        if (log.isDebugEnabled()) {
            log.debug("Headers IP - X-Forwarded-For: {}, X-Real-IP: {}, RemoteAddr: {}",
                xForwardedFor, xRealIp, remoteAddr);
        }

        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
