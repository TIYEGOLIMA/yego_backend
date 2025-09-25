package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.service.yego_principal.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controlador REST para autenticación del sistema YEGO Principal
 * Equivalente a AuthController de NestJS
 */
@Slf4j
@RestController
@RequestMapping("/api/yego-principal/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Iniciar sesión
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginDto, 
                                  HttpServletRequest request) {
        try {
            LoginResponseDto response = authService.login(loginDto, request);
            
            log.info("✅ Login exitoso para usuario: {}", loginDto.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error en login para usuario {}: {}", loginDto.getUsername(), e.getMessage());
            
            if (e.getMessage().contains("Credenciales inválidas")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Credenciales inválidas"));
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Registrar nuevo usuario
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDto registerDto) {
        try {
            UserResponseDto response = authService.register(registerDto);
            
            log.info("✅ Usuario registrado exitosamente: {}", registerDto.getUsername());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("❌ Error registrando usuario {}: {}", registerDto.getUsername(), e.getMessage());
            
            if (e.getMessage().contains("ya existe")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", e.getMessage()));
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener perfil del usuario autenticado
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            UserProfileDto profile = authService.getUserProfile(userId);
            
            log.info("📤 Perfil devuelto para usuario: {}", profile.getUsername());
            
            return ResponseEntity.ok(profile);
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo perfil: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Cambiar contraseña del usuario autenticado
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDto changePasswordDto,
                                           Authentication authentication) {
        try {
            // Validar que las contraseñas coincidan
            if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Las contraseñas no coinciden"));
            }
            
            Long userId = Long.parseLong(authentication.getName());
            authService.changePassword(userId, changePasswordDto.getCurrentPassword(), 
                    changePasswordDto.getNewPassword());
            
            log.info("✅ Contraseña cambiada exitosamente para usuario ID: {}", userId);
            
            return ResponseEntity.ok(Map.of("message", "Contraseña cambiada exitosamente"));
            
        } catch (Exception e) {
            log.error("❌ Error cambiando contraseña: {}", e.getMessage());
            
            if (e.getMessage().contains("incorrecta") || e.getMessage().contains("inválidas")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", e.getMessage()));
            }
            
            if (e.getMessage().contains("requisitos") || e.getMessage().contains("igual")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", e.getMessage()));
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Cambiar contraseña inicial
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ChangePasswordDto changePasswordDto) {
        try {
            // Validar que las contraseñas coincidan
            if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Las contraseñas no coinciden"));
            }
            
            authService.resetPassword(changePasswordDto);
            
            log.info("✅ Contraseña inicial cambiada exitosamente para usuario: {}", 
                    changePasswordDto.getUsername());
            
            return ResponseEntity.ok(Map.of("message", "Contraseña cambiada exitosamente"));
            
        } catch (Exception e) {
            log.error("❌ Error en reset de contraseña: {}", e.getMessage());
            
            if (e.getMessage().contains("Credenciales inválidas")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Credenciales inválidas"));
            }
            
            if (e.getMessage().contains("no requiere cambio")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", e.getMessage()));
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Cerrar sesión completa y liberar módulo
     */
    @PostMapping("/logout")
    public ResponseEntity<?> cerrarSesion(HttpServletRequest request, 
                                         Authentication authentication) {
        log.info("🔓 Iniciando logout");
        
        try {
            // Extraer token del header Authorization
            String authHeader = request.getHeader("Authorization");
            String token = authHeader != null ? authHeader.replace("Bearer ", "") : "";
            
            if (token.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "Logout exitoso - no hay token activo",
                        "success", true,
                        "timestamp", LocalDateTime.now()
                ));
            }
            
            Long userId = null;
            String username = "usuario";
            
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    userId = Long.parseLong(authentication.getName());
                    username = authentication.getPrincipal().toString();
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo obtener información del usuario autenticado");
                }
            }
            
            authService.cerrarSesion(userId, token);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Logout exitoso, módulo liberado",
                    "success", true,
                    "timestamp", LocalDateTime.now(),
                    "user", username
            ));
            
        } catch (Exception e) {
            log.error("❌ Error en logout: {}", e.getMessage());
            // Siempre devolver éxito en logout para evitar loops infinitos
            return ResponseEntity.ok(Map.of(
                    "message", "Logout completado con advertencias",
                    "success", true,
                    "timestamp", LocalDateTime.now(),
                    "warning", "Algunos recursos no pudieron ser liberados"
            ));
        }
    }
    
    /**
     * Forzar logout sin autenticación (para casos de emergencia)
     */
    @PostMapping("/force-logout")
    public ResponseEntity<?> forceLogout(HttpServletRequest request) {
        log.info("🚨 Logout forzado solicitado");
        
        try {
            // Extraer token del header Authorization si existe
            String authHeader = request.getHeader("Authorization");
            String token = authHeader != null ? authHeader.replace("Bearer ", "") : "";
            
            if (!token.isEmpty()) {
                // Intentar liberar recursos con el token disponible
                authService.cerrarSesion(null, token);
            }
            
            return ResponseEntity.ok(Map.of(
                    "message", "Logout forzado exitoso",
                    "success", true,
                    "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("❌ Error en logout forzado: {}", e.getMessage());
            // Siempre devolver éxito para evitar loops
            return ResponseEntity.ok(Map.of(
                    "message", "Logout forzado completado",
                    "success", true,
                    "timestamp", LocalDateTime.now()
            ));
        }
    }
}
