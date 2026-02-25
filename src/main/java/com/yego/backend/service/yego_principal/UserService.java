package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import org.springframework.http.ResponseEntity;

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
    

    /**
     * Obtener todos los usuarios con paginación
     */
    Object findAll(Integer page, Integer limit, String search, Boolean active);

    /**
     * Listado de usuarios: usuario, rol, esJefe, area, nombre, apellido, email. Sin paginación.
     */
    List<UsuarioResumenDto> findAllResumen();
    
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
    ResponseEntity<?> update(Long id, UpdateUserDto updateUserDto);

    /**
     * Actualizar solo el área del usuario (asignar o quitar de un área).
     * areaId = null o 0 para quitar del área.
     */
    UserResponseDto updateArea(Long id, Long areaId);

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
    
    /**
     * Consultar DNI
     */
    ResponseEntity<DniResponseDto> consultarDni(String dni);
}

