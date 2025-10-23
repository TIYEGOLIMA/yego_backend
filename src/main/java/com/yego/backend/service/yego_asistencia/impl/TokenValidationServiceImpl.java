package com.yego.backend.service.yego_asistencia.impl;

import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_asistencia.TokenValidationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenValidationServiceImpl implements TokenValidationService {

    private final UserRepository userRepository;

    @Value("${jwt.secret:yego_super_secret_key_2025}")
    private String jwtSecret;

    @Override
    public boolean validateToken(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return false;
            }
            
            String jwt = token.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
            
            Long userId = Long.valueOf(claims.getSubject());
            return userRepository.findById(userId).isPresent();
            
        } catch (Exception e) {
            log.warn("⚠️ [TokenValidationService] Error validando token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public User getUserByToken(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return null;
            }
            
            String jwt = token.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
            
            Long userId = Long.valueOf(claims.getSubject());
            return userRepository.findById(userId).orElse(null);
            
        } catch (Exception e) {
            log.warn("⚠️ [TokenValidationService] Error obteniendo usuario del token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Optional<User> getUserFromToken(String token) {
        User user = getUserByToken(token);
        return Optional.ofNullable(user);
    }

    @Override
    public Map<String, Object> getTokenClaims(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return new HashMap<>();
            }
            
            String jwt = token.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
            
            Map<String, Object> claimsMap = new HashMap<>();
            claimsMap.put("userId", claims.getSubject());
            claimsMap.put("username", claims.get("username"));
            claimsMap.put("role", claims.get("role"));
            claimsMap.put("exp", claims.getExpiration());
            
            return claimsMap;
            
        } catch (Exception e) {
            log.warn("⚠️ [TokenValidationService] Error obteniendo claims del token: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public String[] getUserRoles(String token) {
        try {
            Map<String, Object> claims = getTokenClaims(token);
            String role = (String) claims.get("role");
            return role != null ? new String[]{role} : new String[]{};
        } catch (Exception e) {
            log.warn("⚠️ [TokenValidationService] Error obteniendo roles del token: {}", e.getMessage());
            return new String[]{};
        }
    }

    @Override
    public boolean hasRole(String username, String role) {
        log.info("🔐 [TokenValidationService] Verificando rol {} para usuario: {}", role, username);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean hasRole = user.getRole().equals(role);
            log.info("✅ [TokenValidationService] Usuario {} tiene rol {}: {}", username, role, hasRole);
            return hasRole;
        }
        log.warn("⚠️ [TokenValidationService] Usuario {} no encontrado", username);
        return false;
    }

    @Override
    public boolean isAuthenticated(String token) {
        return validateToken(token);
    }
}
