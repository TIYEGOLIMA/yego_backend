package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_principal.api.response.LoginResponseDto;
import com.yego.backend.service.yego_principal.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador REST para autenticación del módulo Ticketera
 * Usa el mismo servicio de autenticación que el módulo principal
 */
@Slf4j
@RestController
@RequestMapping("/api/ticketera/auth")
@RequiredArgsConstructor
public class TicketeraAuthController {
    
    private final AuthService authService;
    
    /**
     * Renovar token JWT para módulo Ticketera
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Token de autorización requerido");
        }
        
        String token = authHeader.substring(7);
        try {
            LoginResponseDto response = authService.refreshToken(token, request);
            log.info("🔄 [Ticketera] Token renovado exitosamente para usuario: {}", response.getUser().getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("❌ [Ticketera] Error renovando token: {}", e.getMessage());
            return ResponseEntity.status(401).body("Token inválido o expirado");
        }
    }
    
    /**
     * Verificar estado del JWT
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Token de autorización requerido");
        }
        
        String token = authHeader.substring(7);
        try {
            // Intentar renovar el token para verificar si es válido
            authService.refreshToken(token, request);
            return ResponseEntity.ok().body("Token válido");
        } catch (Exception e) {
            log.warn("❌ [Ticketera] Token inválido: {}", e.getMessage());
            return ResponseEntity.status(401).body("Token inválido o expirado");
        }
    }
}
