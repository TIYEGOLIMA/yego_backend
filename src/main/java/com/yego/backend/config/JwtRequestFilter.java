package com.yego.backend.config;

import com.yego.backend.service.yego_principal.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Filtro para validación de tokens JWT en requests HTTP
 * Equivalente a JwtAuthGuard de NestJS
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    
    private final AuthService authService;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain chain) throws ServletException, IOException {
        
        String username = null;
        String jwtToken = null;
        
        // CRÍTICO: Para WebSocket nativo, el token puede venir en la URL query parameter
        // Formato: /ws?token={token}
        // Esto es necesario porque WebSocket nativo NO permite headers HTTP personalizados durante el handshake
        if (request.getRequestURI().equals("/ws")) {
            String queryString = request.getQueryString();
            if (queryString != null && queryString.contains("token=")) {
                try {
                    String[] params = queryString.split("&");
                    for (String param : params) {
                        if (param.startsWith("token=")) {
                            jwtToken = param.substring(6); // "token=" tiene 6 caracteres
                            // Decodificar URL encoding
                            jwtToken = URLDecoder.decode(jwtToken, StandardCharsets.UTF_8);
                            log.info("🔑 [JwtRequestFilter] Token JWT recibido desde URL query parameter para: {}", request.getRequestURI());
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [JwtRequestFilter] Error decodificando token de URL: {}", e.getMessage());
                }
            }
        }
        
        // Si no se encontró en la URL, intentar leer del header Authorization (para HTTP normal)
        if (jwtToken == null) {
            final String requestTokenHeader = request.getHeader("Authorization");
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);
                log.info("🔑 [JwtRequestFilter] Token JWT recibido desde header para: {}", request.getRequestURI());
            }
        }
        
        // Procesar el token si se encontró
        if (jwtToken != null) {
            try {
                // Usar API moderna de JWT (consistente con el resto del código)
                SecretKey key = getSigningKey();
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(jwtToken)
                        .getPayload();
                
                username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                log.info("✅ [JwtRequestFilter] Usuario autenticado: {} con rol: {}", username, role);
                
                // Almacenar claims en el request para uso posterior
                request.setAttribute("jwtClaims", claims);
                
            } catch (Exception e) {
                // Si el token está expirado, responder con 401 y mensaje claro para el frontend
                if (e.getMessage() != null && (e.getMessage().contains("expired") || e.getMessage().contains("expiration"))) {
                    log.debug("🕐 [JwtRequestFilter] Token expirado para: {} - El frontend debe renovar el token", request.getRequestURI());
                    
                    // Responder con 401 y mensaje claro para que el frontend detecte y renueve el token
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"TOKEN_EXPIRED\",\"message\":\"El token ha expirado. Por favor, renueve su sesión.\"}");
                    return; // Detener el procesamiento - el frontend debe manejar esto
                } else {
                    // Para otros errores, loggear en WARN
                    log.warn("❌ [JwtRequestFilter] Error procesando JWT para {}: {}", request.getRequestURI(), e.getMessage());
                }
            }
        } else {
            // Para /ws, BLOQUEAR conexiones sin token (no solo loggear warning)
            if (request.getRequestURI().equals("/ws")) {
                String queryString = request.getQueryString();
                if (queryString == null || !queryString.contains("token=")) {
                    // Usar DEBUG en lugar de WARN para evitar spam en logs durante reconexiones
                    // El frontend debe manejar esto correctamente, no es un error del servidor
                    log.debug("🚫 [JwtRequestFilter] Conexión WebSocket rechazada: No se recibió token (ni en header ni en URL)");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Token de autenticación requerido\"}");
                    return; // BLOQUEAR la conexión
                }
            } else {
                // Para otras rutas, solo loggear warning
                log.warn("⚠️ [JwtRequestFilter] No se recibió token Bearer para: {}", request.getRequestURI());
            }
        }
        
        // Una vez que obtenemos el token, validamos
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            UserDetails userDetails = authService.loadUserByUsername(username);
            
            // Si el token es válido, configurar Spring Security para establecer manualmente la autenticación
            if (validateToken(jwtToken, userDetails)) {
                
                // Obtener userId del token (puede venir como Long o Integer en el JWT)
                String userIdStr = null;
                Long userIdLong = null;
                try {
                    Claims tokenClaims = (Claims) request.getAttribute("jwtClaims");
                    if (tokenClaims != null) {
                        Long uid = tokenClaims.get("userId", Long.class);
                        if (uid == null) {
                            Integer uidInt = tokenClaims.get("userId", Integer.class);
                            if (uidInt != null) userIdLong = uidInt.longValue();
                        } else {
                            userIdLong = uid;
                        }
                        if (userIdLong != null) userIdStr = userIdLong.toString();
                    }
                    if (userIdStr == null) userIdStr = username;
                } catch (Exception e) {
                    log.warn("No se pudo obtener userId del token: {}", e.getMessage());
                    userIdStr = username;
                }
                
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = 
                        new UsernamePasswordAuthenticationToken(
                                userIdStr, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                log.info("🔐 [JwtRequestFilter] Authorities asignadas: {}", userDetails.getAuthorities());
                
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                
                // Política semanal: si la contraseña está vencida, solo permitir cambio de contraseña, reset o logout
                // Solo aplicar si tenemos un userId numérico (excluidos: 1, 4, 5, 6 no tienen esta obligación)
                String path = request.getRequestURI();
                boolean pathAllowed = path.contains("/change-password") || path.contains("/reset-password") || path.contains("/logout");
                if (!pathAllowed && userIdLong != null) {
                    try {
                        if (authService.isPasswordExpired(userIdLong)) {
                            log.info("🔒 [JwtRequestFilter] Acceso bloqueado: contraseña vencida (política semanal) para usuario {}", userIdLong);
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"PASSWORD_EXPIRED\",\"message\":\"Debes cambiar tu contraseña para continuar usando Integral.\"}");
                            return;
                        }
                    } catch (Exception e) {
                        log.warn("Error comprobando expiración de contraseña para userId {}: {}", userIdLong, e.getMessage());
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
    
    private Boolean validateToken(String token, UserDetails userDetails) {
        try {
            // Ya validamos el token arriba, solo verificamos que el username coincida
            // y que el usuario esté activo
            return userDetails != null && userDetails.isEnabled();
            
        } catch (Exception e) {
            log.warn("Error validando token: {}", e.getMessage());
            return false;
        }
    }
    
}
