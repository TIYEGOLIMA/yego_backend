package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Interfaz del servicio de autenticación del sistema YEGO Principal
 * Equivalente a AuthService de NestJS
 */
public interface AuthService extends UserDetailsService {
    
    /**
     * Validar usuario con username y password
     */
    UserResponseDto validateUser(String username, String password, HttpServletRequest request);
    
    /**
     * Realizar login y generar token
     */
    LoginResponseDto login(LoginDto loginDto, HttpServletRequest request);
    
    /**
     * Registrar nuevo usuario
     */
    UserResponseDto register(RegisterDto registerDto);
    
    /**
     * Cambiar contraseña de usuario autenticado
     */
    void changePassword(Long userId, String currentPassword, String newPassword);
    
    /**
     * Reset de contraseña inicial
     */
    void resetPassword(ChangePasswordDto changePasswordDto);
    
    /**
     * Validar fortaleza de contraseña
     */
    boolean validatePassword(String password);
    
    /**
     * Obtener perfil de usuario
     */
    UserProfileDto getUserProfile(Long userId);
    
    /**
     * Decodificar token sin verificar
     */
    Object decodeToken(String token);
    
    /**
     * Cerrar sesión y liberar recursos
     */
    void cerrarSesion(Long userId, String token);
    
    /**
     * Cargar usuario por username para Spring Security
     */
    UserDetails loadUserByUsername(String username);
}

