package com.yego.backend.config;

import com.yego.backend.entity.yego_ticketerera.entities.Dispositivo;
import com.yego.backend.repository.yego_ticketerera.DispositivoRepository;
import com.yego.backend.service.yego_principal.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import java.util.List;

/**
 * Filtro para validación de tokens JWT en requests HTTP
 * Equivalente a JwtAuthGuard de NestJS
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    
    private final AuthService authService;
    private final DispositivoRepository dispositivoRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/api/auth/refresh".equals(uri)
                || "/api/ticketera/auth/refresh".equals(uri);
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain chain) throws ServletException, IOException {
        
        // Preflight CORS: no JWT; debe llegar al CorsFilter sin tocar la respuesta
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // El JWT no viaja en la URL. Un ticket efímero se valida y consume en STOMP CONNECT.
        if (request.getRequestURI().equals("/ws") && hasQueryParameter(request, "ticket")) {
            chain.doFilter(request, response);
            return;
        }
        
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
                            log.debug("[JwtRequestFilter] Token JWT recibido por URL para {}", request.getRequestURI());
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.debug("[JwtRequestFilter] Token URL no decodificable para {}", request.getRequestURI());
                }
            }
        }
        
        // Si no se encontró en la URL, intentar leer del header Authorization (para HTTP normal)
        if (jwtToken == null) {
            final String requestTokenHeader = request.getHeader("Authorization");
            if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);
                log.debug("[JwtRequestFilter] Token JWT recibido por header para {}", request.getRequestURI());
            }
        }
        
        // Procesar el token si se encontró
        if (jwtToken != null) {
            try {
                // Usar API moderna de JWT (consistente con el resto del código)
                Claims claims = jwtTokenProvider.parse(jwtToken);
                
                Long dispositivoIdClaim = getLongClaim(claims, "dispositivoId");
                if (dispositivoIdClaim != null) {
                    Integer tokenVersionClaim = getIntegerClaim(claims, "tokenVersion");
                    Dispositivo dispositivo = dispositivoRepository.findById(dispositivoIdClaim).orElse(null);
                    if (dispositivo == null || Boolean.FALSE.equals(dispositivo.getActive())) {
                        log.debug("[JwtRequestFilter] Dispositivo inactivo o eliminado: {}", dispositivoIdClaim);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"DEVICE_REVOKED\",\"message\":\"Dispositivo desactivado\"}");
                        return;
                    }
                    int versionActual = dispositivo.getTokenVersion() != null ? dispositivo.getTokenVersion() : 0;
                    int versionToken = tokenVersionClaim != null ? tokenVersionClaim : 0;
                    if (versionActual != versionToken) {
                        log.debug("[JwtRequestFilter] Token revocado dispositivo={} (token={}, actual={})",
                                dispositivoIdClaim, versionToken, versionActual);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"DEVICE_TOKEN_REVOKED\",\"message\":\"Sesión del dispositivo revocada\"}");
                        return;
                    }
                    request.setAttribute("jwtClaims", claims);
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken deviceAuthentication =
                                new UsernamePasswordAuthenticationToken(
                                        "device-" + dispositivoIdClaim,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
                                );
                        deviceAuthentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(deviceAuthentication);
                    }
                    chain.doFilter(request, response);
                    return;
                }

                username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                String tokenType = claims.get("type", String.class);
                String driverId = claims.get("driverId", String.class);
                if (username != null) {
                    log.debug("[JwtRequestFilter] Usuario autenticado: {} rol={}", username, role);
                } else if ("mobile_driver".equals(tokenType) || "CONDUCTOR".equals(role)) {
                    log.debug("[JwtRequestFilter] Conductor movil autenticado: driverId={} rol={} type={}",
                            firstNonBlank(driverId, claims.getSubject()), role, tokenType);
                } else {
                    log.debug("[JwtRequestFilter] Token valido sin username: subject={} rol={} type={}",
                            claims.getSubject(), role, tokenType);
                }
                
                // Almacenar claims en el request para uso posterior
                request.setAttribute("jwtClaims", claims);

                if (username == null
                        && ("mobile_driver".equals(tokenType) || "CONDUCTOR".equals(role))
                        && driverId != null
                        && !driverId.isBlank()
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken mobileAuthentication =
                            new UsernamePasswordAuthenticationToken(
                                    driverId,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_CONDUCTOR"))
                            );
                    mobileAuthentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(mobileAuthentication);
                }
                
            } catch (ExpiredJwtException e) {
                log.debug("[JwtRequestFilter] Token expirado para {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"TOKEN_EXPIRED\",\"message\":\"El token ha expirado. Por favor, renueve su sesión.\"}");
                return;
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("[JwtRequestFilter] Token rechazado para {} ({})",
                        request.getRequestURI(), e.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("[JwtRequestFilter] Fallo inesperado procesando JWT para {}",
                        request.getRequestURI(), e);
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
            } else if (esRutaAuthSinBearerEsperado(request.getRequestURI())) {
                log.debug("[JwtRequestFilter] Sin Bearer en ruta anónima: {}", request.getRequestURI());
            } else {
                log.debug("[JwtRequestFilter] Solicitud sin Bearer para {}", request.getRequestURI());
            }
        }
        
        // Una vez que obtenemos el token, validamos
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            UserDetails userDetails;
            try {
                userDetails = authService.loadUserByUsername(username);
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException exception) {
                log.debug("[JwtRequestFilter] Usuario del token ya no existe");
                chain.doFilter(request, response);
                return;
            }
            
            // Si el token es válido, configurar Spring Security para establecer manualmente la autenticación
            if (userDetails.isEnabled()) {
                
                // Obtener userId del token (puede venir como Long o Integer en el JWT)
                String userIdStr = null;
                Long userIdLong = null;
                try {
                    Claims tokenClaims = (Claims) request.getAttribute("jwtClaims");
                    if (tokenClaims != null) {
                        userIdLong = getLongClaim(tokenClaims, "userId");
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
                
                log.debug("[JwtRequestFilter] Authorities asignadas: {}", userDetails.getAuthorities());
                
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                
                // Política semanal: si la contraseña está vencida, solo permitir cambio de contraseña, reset o logout.
                // Exclusiones: auth.password-policy.excluded-user-ids (p. ej. 1, 4, 5, 6, 27, 37).
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
    
    private boolean esRutaAuthSinBearerEsperado(String uri) {
        if (uri == null) {
            return false;
        }
        return uri.contains("/auth/login")
                || uri.contains("/auth/register")
                || uri.contains("/dispositivos/auth")
                || uri.startsWith("/api/mobile/")
                || uri.startsWith("/api/yango-external/");
    }

    private static Long getLongClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value instanceof Number number ? number.longValue() : null;
    }

    private static boolean hasQueryParameter(HttpServletRequest request, String name) {
        String query = request.getQueryString();
        if (query == null || query.isBlank()) return false;
        String prefix = name + "=";
        for (String parameter : query.split("&")) {
            if (parameter.startsWith(prefix) && parameter.length() > prefix.length()) return true;
        }
        return false;
    }

    private static Integer getIntegerClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value instanceof Number number ? number.intValue() : null;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
    
}
