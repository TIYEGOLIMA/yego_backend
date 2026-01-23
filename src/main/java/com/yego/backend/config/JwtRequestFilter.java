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
                log.warn("❌ [JwtRequestFilter] Error procesando JWT para {}: {}", request.getRequestURI(), e.getMessage());
                // Si el token está expirado, no continuar con la autenticación
                if (e.getMessage() != null && (e.getMessage().contains("expired") || e.getMessage().contains("expiration"))) {
                    log.info("🕐 [JwtRequestFilter] Token expirado para: {}", request.getRequestURI());
                }
            }
        } else {
            // Solo loggear warning si no es /ws o si es /ws pero tampoco tiene token en URL
            if (!request.getRequestURI().equals("/ws")) {
                log.warn("⚠️ [JwtRequestFilter] No se recibió token Bearer para: {}", request.getRequestURI());
            } else {
                // Para /ws, verificar si hay token en la URL
                String queryString = request.getQueryString();
                if (queryString == null || !queryString.contains("token=")) {
                    log.warn("⚠️ [JwtRequestFilter] No se recibió token Bearer para: /ws (ni en header ni en URL)");
                }
            }
        }
        
        // Una vez que obtenemos el token, validamos
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            UserDetails userDetails = authService.loadUserByUsername(username);
            
            // Si el token es válido, configurar Spring Security para establecer manualmente la autenticación
            if (validateToken(jwtToken, userDetails)) {
                
                // Obtener userId del token para usarlo como nombre de autenticación
                // Reutilizar los claims ya parseados del request attribute
                String userId = null;
                try {
                    Claims tokenClaims = (Claims) request.getAttribute("jwtClaims");
                    if (tokenClaims != null) {
                        // El userId viene como Integer en el token, lo convertimos a String
                        Integer userIdInt = tokenClaims.get("userId", Integer.class);
                        userId = userIdInt != null ? userIdInt.toString() : null;
                    }
                    if (userId == null) {
                        userId = username; // Fallback al username si no se puede obtener userId
                    }
                } catch (Exception e) {
                    log.warn("No se pudo obtener userId del token: {}", e.getMessage());
                    userId = username; // Fallback al username si no se puede obtener userId
                }
                
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = 
                        new UsernamePasswordAuthenticationToken(
                                userId, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                log.info("🔐 [JwtRequestFilter] Authorities asignadas: {}", userDetails.getAuthorities());
                
                // Después de establecer la autenticación en el contexto, especificamos
                // que el usuario actual está autenticado. Así pasa los filtros de Spring Security.
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
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
