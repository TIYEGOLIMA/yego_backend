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
        
        final String requestTokenHeader = request.getHeader("Authorization");
        
        String username = null;
        String jwtToken = null;
        
        // JWT Token está en la forma "Bearer token". Remover la palabra Bearer y obtener solo el token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            log.info("🔑 [JwtRequestFilter] Token JWT recibido para: {}", request.getRequestURI());
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
            log.warn("⚠️ [JwtRequestFilter] No se recibió token Bearer para: {}", request.getRequestURI());
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

