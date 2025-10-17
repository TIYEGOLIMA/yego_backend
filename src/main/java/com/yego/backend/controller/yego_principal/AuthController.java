package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Controlador REST para autenticación del sistema YEGO Principal
 * Equivalente a AuthController de NestJS
 */

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Iniciar sesión
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginDto, 
                                  HttpServletRequest request) {
        LoginResponseDto response = authService.login(loginDto, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Registrar nuevo usuario
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDto registerDto) {
        UserResponseDto response = authService.register(registerDto);
        return ResponseEntity.status(201).body(response);
    }
    
    /**
     * Obtener perfil del usuario autenticado
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserProfileDto profile = authService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * Cambiar contraseña del usuario autenticado
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDto changePasswordDto,
                                           Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            authService.changePassword(userId, changePasswordDto.getCurrentPassword(), 
                    changePasswordDto.getNewPassword());
            return ResponseEntity.ok().body("Contraseña cambiada exitosamente");
        } catch (Exception e) {
            log.error("Error al cambiar contraseña: {}", e.getMessage());
            throw e; // Re-lanzar para que Spring maneje la ResponseStatusException
        }
    }
    
    /**
     * Cambiar contraseña inicial
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ChangePasswordDto changePasswordDto) {
        try {
            authService.resetPassword(changePasswordDto);
            return ResponseEntity.ok().body("Contraseña cambiada exitosamente");
        } catch (Exception e) {
            log.error("Error al cambiar contraseña inicial: {}", e.getMessage());
            throw e; // Re-lanzar para que Spring maneje la ResponseStatusException
        }
    }
    
    /**
     * Cerrar sesión completa y liberar módulo
     */
    @PostMapping("/logout")
    public ResponseEntity<?> cerrarSesion(HttpServletRequest request, 
                                         Authentication authentication) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader != null ? authHeader.replace("Bearer ", "") : "";
        
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userId = Long.parseLong(authentication.getName());
        }
        
        authService.cerrarSesion(userId, token);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Forzar logout sin autenticación (para casos de emergencia)
     */
    @PostMapping("/force-logout")
    public ResponseEntity<?> forceLogout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader != null ? authHeader.replace("Bearer ", "") : "";
        
        if (!token.isEmpty()) {
            authService.cerrarSesion(null, token);
        }
        
        return ResponseEntity.ok().build();
    }
}

