package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.Permission;

import java.util.List;

/**
 * Interfaz del servicio de permisos del sistema YEGO Principal
 * Equivalente a PermissionsService de NestJS
 */
public interface PermissionService {
    
    /**
     * Crear nuevo permiso
     */
    PermissionResponseDto create(CreatePermissionDto createPermissionDto);
    
    /**
     * Obtener todos los permisos
     */
    List<PermissionResponseDto> findAll();
    
    /**
     * Obtener todos los permisos activos
     */
    List<PermissionResponseDto> findAllActive();
    
    /**
     * Obtener permisos por módulo
     */
    List<PermissionResponseDto> findByModule(String module);
    
    /**
     * Obtener permiso por ID
     */
    PermissionResponseDto findOne(Long id);
    
    /**
     * Buscar permiso por nombre
     */
    Permission findByName(String name);
    
    /**
     * Actualizar permiso
     */
    PermissionResponseDto update(Long id, UpdatePermissionDto updatePermissionDto);
    
    /**
     * Eliminar permiso
     */
    void remove(Long id);
    
    /**
     * Verificar si un usuario tiene un permiso específico
     */
    boolean checkPermission(Long userId, String permissionName);
}