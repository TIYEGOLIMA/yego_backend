package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.Permission;
import com.yego.backend.repository.yego_principal.PermissionRepository;
import com.yego.backend.service.yego_principal.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
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
                .active(createPermissionDto.getActive() != null ? createPermissionDto.getActive() : true)
                .build();
        
        Permission savedPermission = permissionRepository.save(permission);
        
        log.info("✅ Permiso YEGO Principal creado: {}", savedPermission.getName());
        
        return mapToResponseDto(savedPermission);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponseDto> findAll() {
        // Todos los permisos (activos e inactivos) para la pantalla de gestión con filtro por estado
        Sort sort = Sort.by("module").ascending().and(Sort.by("action").ascending());
        List<Permission> permissions = permissionRepository.findAll(sort);
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

    private PermissionResponseDto mapToResponseDto(Permission permission) {
        return PermissionResponseDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .module(permission.getModule())
                .action(permission.getAction())
                .active(permission.getActive())
                .created_at(permission.getCreatedAt())
                .updated_at(permission.getUpdatedAt())
                .build();
    }
}

