package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;

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
     * Obtener todos los permisos activos (usado por RoleServiceImpl para módulos con acciones)
     */
    List<PermissionResponseDto> findAllActive();

    /**
     * Actualizar permiso
     */
    PermissionResponseDto update(Long id, UpdatePermissionDto updatePermissionDto);
    
    /**
     * Eliminar permiso (soft delete)
     */
    void remove(Long id);
}