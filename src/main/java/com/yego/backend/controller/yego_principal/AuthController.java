package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.AuthService;
import com.yego.backend.service.yego_principal.ModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

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
    private final ModuleService moduleService;
    
    /**
     * Iniciar sesión
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginDto, 
                                  HttpServletRequest request) {
        LoginTokenResult result = authService.login(loginDto, request);
        LoginResponseDto body = LoginResponseDto.builder().message(result.message()).build();
        return ResponseEntity.ok()
                .header("X-Access-Token", result.accessToken())
                .body(body);
    }
    
    /**
     * Renovar token JWT
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Token de autorización requerido");
        }
        
        String token = authHeader.substring(7);
        LoginTokenResult result = authService.refreshToken(token, request);
        LoginResponseDto body = LoginResponseDto.builder().message(result.message()).build();
        return ResponseEntity.ok()
                .header("X-Access-Token", result.accessToken())
                .body(body);
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
    public ResponseEntity<?> getProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserProfileDto profile = authService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * Cambiar contraseña del usuario autenticado
     */
    @PostMapping("/change-password")
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
     * Cambiar contraseña inicial (p. ej. desde modal de cambio obligatorio)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ChangePasswordDto changePasswordDto,
                                            HttpServletRequest request) {
        try {
            authService.resetPassword(changePasswordDto, request);
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
    
    /**
     * Obtener módulos permitidos para el usuario autenticado según su rol
     * El frontend puede usar este endpoint para mostrar solo las opciones disponibles
     */
    @GetMapping("/my-modules")
    public ResponseEntity<List<ModuleResponse>> getMyModules(Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("📋 [AuthController] Obteniendo módulos para usuario ID: {}", userId);
            List<ModuleResponse> modules = moduleService.obtenerModulosPorUsuario(userId);
            log.info("✅ [AuthController] Devueltos {} módulos para usuario {}", modules.size(), userId);
            return ResponseEntity.ok(modules);
        } catch (Exception e) {
            log.error("❌ [AuthController] Error obteniendo módulos del usuario: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

