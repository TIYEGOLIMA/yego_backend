package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.Role;

import java.util.List;

/**
 * Interfaz del servicio de roles del sistema YEGO Principal
 * Equivalente a RolesService de NestJS
 */
public interface RoleService {
    
    /**
     * Crear nuevo rol
     */
    RoleResponseDto create(CreateRoleDto createRoleDto);
    
    /**
     * Obtener todos los roles
     */
    List<RoleResponseDto> findAll();

    /**
     * Obtener roles activos (solo id y name - para formularios)
     */
    List<RoleSimpleDto> findAllActive();
    
    /**
     * Obtener rol por ID
     */
    RoleResponseDto findOne(Long id);
    
    /**
     * Buscar rol por nombre
     */
    Role findByName(String name);
    
    /**
     * Actualizar rol
     */
    RoleResponseDto update(Long id, UpdateRoleDto updateRoleDto);
    
    /**
     * Eliminar rol
     */
    void remove(Long id);
    
    /**
     * Obtener cantidad de usuarios por rol
     */
    Long getUserCountByRole(String roleName);
    
    /**
     * Obtener roles activos
     */
    List<RoleResponseDto> findActive();
    
    /**
     * Activar/Desactivar rol
     */
    RoleResponseDto toggleStatus(Long id);
    
    /**
     * Obtener módulos con sus acciones disponibles para crear/editar roles
     * Combina información de queue_modulos y permissions
     */
    List<ModuleWithActionsDto> getModulesWithActions();
}

