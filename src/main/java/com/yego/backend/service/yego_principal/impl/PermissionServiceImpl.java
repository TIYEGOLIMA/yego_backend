package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.Permission;
import com.yego.backend.repository.yego_principal.PermissionRepository;
import com.yego.backend.service.yego_principal.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de permisos del sistema YEGO Principal
 * Equivalente a PermissionsService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    
    private final PermissionRepository permissionRepository;
    
    @Override
    @Transactional
    public PermissionResponseDto create(CreatePermissionDto createPermissionDto) {
        // Verificar si el permiso ya existe
        if (permissionRepository.existsByName(createPermissionDto.getName())) {
            throw new IllegalStateException("El permiso '" + createPermissionDto.getName() + "' ya existe");
        }
        
        Permission permission = Permission.builder()
                .name(createPermissionDto.getName())
                .description(createPermissionDto.getDescription())
                .module(createPermissionDto.getModule())
                .action(createPermissionDto.getAction())
                .conditions(createPermissionDto.getConditions())
                .active(createPermissionDto.getActive() != null ? createPermissionDto.getActive() : true)
                .build();
        
        Permission savedPermission = permissionRepository.save(permission);
        
        log.info("✅ Permiso YEGO Principal creado: {}", savedPermission.getName());
        
        return mapToResponseDto(savedPermission);
    }
    
    @Override
    public List<PermissionResponseDto> findAll() {
        List<Permission> permissions = permissionRepository.findByActiveOrderByModuleAscActionAsc(true);
        
        return permissions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponseDto> findAllActive() {
        log.info("📋 [PermissionService] Obteniendo permisos activos ordenados por módulo y acción");
        List<Permission> activePermissions = permissionRepository.findByActiveTrueOrderByModuleAscActionAsc();
        
        return activePermissions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PermissionResponseDto> findByModule(String module) {
        List<Permission> permissions = permissionRepository.findByModuleAndActiveOrderByActionAsc(module, true);
        
        return permissions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public PermissionResponseDto findOne(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permiso con ID " + id + " no encontrado"));
        
        return mapToResponseDto(permission);
    }
    
    @Override
    public Permission findByName(String name) {
        return permissionRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Permiso '" + name + "' no encontrado"));
    }
    
    @Override
    @Transactional
    public PermissionResponseDto update(Long id, UpdatePermissionDto updatePermissionDto) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permiso con ID " + id + " no encontrado"));
        
        // Verificar si el nuevo nombre ya existe (si se está cambiando)
        if (updatePermissionDto.getName() != null && !updatePermissionDto.getName().equals(permission.getName())) {
            if (permissionRepository.existsByName(updatePermissionDto.getName())) {
                throw new IllegalStateException("El permiso '" + updatePermissionDto.getName() + "' ya existe");
            }
        }
        
        // Actualizar campos
        if (updatePermissionDto.getName() != null) {
            permission.setName(updatePermissionDto.getName());
        }
        if (updatePermissionDto.getDescription() != null) {
            permission.setDescription(updatePermissionDto.getDescription());
        }
        if (updatePermissionDto.getModule() != null) {
            permission.setModule(updatePermissionDto.getModule());
        }
        if (updatePermissionDto.getAction() != null) {
            permission.setAction(updatePermissionDto.getAction());
        }
        if (updatePermissionDto.getConditions() != null) {
            permission.setConditions(updatePermissionDto.getConditions());
        }
        if (updatePermissionDto.getActive() != null) {
            permission.setActive(updatePermissionDto.getActive());
        }
        
        Permission savedPermission = permissionRepository.save(permission);
        
        log.info("✅ Permiso YEGO Principal actualizado: {}", savedPermission.getName());
        
        return mapToResponseDto(savedPermission);
    }
    
    @Override
    @Transactional
    public void remove(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permiso con ID " + id + " no encontrado"));
        
        // Soft delete
        permission.setActive(false);
        permissionRepository.save(permission);
        
        log.info("✅ Permiso YEGO Principal desactivado: {}", permission.getName());
    }
    
    @Override
    @Transactional
    public void initializeDefaultPermissions() {
        List<DefaultPermissionData> defaultPermissions = getDefaultPermissionsData();
        
        for (DefaultPermissionData defaultPermission : defaultPermissions) {
            if (!permissionRepository.existsByName(defaultPermission.name)) {
                CreatePermissionDto createPermissionDto = CreatePermissionDto.builder()
                        .name(defaultPermission.name)
                        .description(defaultPermission.description)
                        .module(defaultPermission.module)
                        .action(defaultPermission.action)
                        .active(true)
                        .build();
                
                create(createPermissionDto);
                log.info("⚙️ Permiso por defecto YEGO Principal creado: {}", defaultPermission.name);
            }
        }
    }
    
    @Override
    public boolean checkPermission(Long userId, String permissionName) {
        // Implementación básica - en una implementación completa se verificaría
        // contra los roles del usuario y sus permisos asociados
        try {
            Permission permission = findByName(permissionName);
            return permission.getActive();
        } catch (EntityNotFoundException e) {
            return false;
        }
    }
    
    private PermissionResponseDto mapToResponseDto(Permission permission) {
        return PermissionResponseDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .module(permission.getModule())
                .action(permission.getAction())
                .conditions(permission.getConditions())
                .active(permission.getActive())
                .created_at(permission.getCreatedAt())
                .updated_at(permission.getUpdatedAt())
                .build();
    }
    
    private List<DefaultPermissionData> getDefaultPermissionsData() {
        return Arrays.asList(
                // Users module
                new DefaultPermissionData("users.create", "Crear usuarios", "users", "create"),
                new DefaultPermissionData("users.read", "Ver usuarios", "users", "read"),
                new DefaultPermissionData("users.update", "Actualizar usuarios", "users", "update"),
                new DefaultPermissionData("users.delete", "Eliminar usuarios", "users", "delete"),
                
                // Roles module
                new DefaultPermissionData("roles.create", "Crear roles", "roles", "create"),
                new DefaultPermissionData("roles.read", "Ver roles", "roles", "read"),
                new DefaultPermissionData("roles.update", "Actualizar roles", "roles", "update"),
                new DefaultPermissionData("roles.delete", "Eliminar roles", "roles", "delete"),
                
                // Permissions module
                new DefaultPermissionData("permissions.create", "Crear permisos", "permissions", "create"),
                new DefaultPermissionData("permissions.read", "Ver permisos", "permissions", "read"),
                new DefaultPermissionData("permissions.update", "Actualizar permisos", "permissions", "update"),
                new DefaultPermissionData("permissions.delete", "Eliminar permisos", "permissions", "delete"),
                
                // Modules module
                new DefaultPermissionData("modules.create", "Crear módulos", "modules", "create"),
                new DefaultPermissionData("modules.read", "Ver módulos", "modules", "read"),
                new DefaultPermissionData("modules.update", "Actualizar módulos", "modules", "update"),
                new DefaultPermissionData("modules.delete", "Eliminar módulos", "modules", "delete"),
                
                // Imports module
                new DefaultPermissionData("imports.create", "Crear importaciones", "imports", "create"),
                new DefaultPermissionData("imports.read", "Ver importaciones", "imports", "read"),
                new DefaultPermissionData("imports.update", "Actualizar importaciones", "imports", "update"),
                new DefaultPermissionData("imports.delete", "Eliminar importaciones", "imports", "delete"),
                
                // Audit module
                new DefaultPermissionData("audit.read", "Ver logs de auditoría", "audit", "read"),
                
                // Configuration module
                new DefaultPermissionData("configuration.read", "Ver configuración", "configuration", "read"),
                new DefaultPermissionData("configuration.update", "Actualizar configuración", "configuration", "update"),
                
                // Sessions module
                new DefaultPermissionData("sessions.read", "Ver sesiones", "sessions", "read"),
                new DefaultPermissionData("sessions.delete", "Cerrar sesiones", "sessions", "delete"),
                
                // Reports module
                new DefaultPermissionData("reports.read", "Ver reportes", "reports", "read"),
                new DefaultPermissionData("reports.export", "Exportar reportes", "reports", "export")
        );
    }
    
    private static class DefaultPermissionData {
        final String name;
        final String description;
        final String module;
        final String action;
        
        DefaultPermissionData(String name, String description, String module, String action) {
            this.name = name;
            this.description = description;
            this.module = module;
            this.action = action;
        }
    }
}

