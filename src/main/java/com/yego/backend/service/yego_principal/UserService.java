package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;

import java.util.List;

/**
 * Interfaz del servicio de usuarios del sistema YEGO Principal
 * Equivalente a UsersService de NestJS
 */
public interface UserService {
    
    /**
     * Crear nuevo usuario
     */
    UserResponseDto create(CreateUserDto createUserDto);
    
    Object findAll(Integer page, Integer limit, String search, Boolean active);
    
    /**
     * Obtener usuario por ID
     */
    UserResponseDto findOne(Long id);
    
    /**
     * Buscar usuario por username
     */
    UserResponseDto findByUsername(String username);
    
    /**
     * Actualizar usuario
     */
    UserResponseDto update(Long id, UpdateUserDto updateUserDto);
    
    /**
     * Eliminar usuario (soft delete)
     */
    void remove(Long id);

    /**
     * Cambiar estado de usuario
     */
    UserResponseDto cambiarEstado(Long id, Boolean activo);
    
    /**
     * Cambiar contraseña de usuario
     */
    void changePassword(Long id, String newPassword);
    
    /**
     * Validar fortaleza de contraseña
     */
    boolean validatePassword(String password);
}

