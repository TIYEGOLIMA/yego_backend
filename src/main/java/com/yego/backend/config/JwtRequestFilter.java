package com.yego.backend.config;

import com.yego.backend.service.yego_principal.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
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
    
    private JwtParser getJwtParser() {
        return Jwts.parser().setSigningKey(getSigningKey()).build();
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
            try {
                Claims claims = getJwtParser()
                        .parseClaimsJws(jwtToken)
                        .getBody();
                
                username = claims.get("username", String.class);
                
                // Almacenar claims en el request para uso posterior
                request.setAttribute("jwtClaims", claims);
                
            } catch (Exception e) {
                log.warn("No se pudo obtener el username del token JWT: {}", e.getMessage());
            }
        } else {
            log.debug("JWT Token no comienza con Bearer String");
        }
        
        // Una vez que obtenemos el token, validamos
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            UserDetails userDetails = authService.loadUserByUsername(username);
            
            // Si el token es válido, configurar Spring Security para establecer manualmente la autenticación
            if (validateToken(jwtToken, userDetails)) {
                
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = 
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Después de establecer la autenticación en el contexto, especificamos
                // que el usuario actual está autenticado. Así pasa los filtros de Spring Security.
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        chain.doFilter(request, response);
    }
    
    private Boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = getJwtParser()
                    .parseClaimsJws(token)
                    .getBody();
            
            String username = claims.get("username", String.class);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(claims));
            
        } catch (Exception e) {
            log.warn("Error validando token: {}", e.getMessage());
            return false;
        }
    }
    
    private Boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new java.util.Date());
    }
}

