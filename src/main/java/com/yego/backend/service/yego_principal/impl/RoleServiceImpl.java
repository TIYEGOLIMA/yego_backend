package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.Role;
import com.yego.backend.repository.yego_principal.RoleRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_principal.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de roles del sistema YEGO Principal
 * Equivalente a RolesService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public RoleResponseDto create(CreateRoleDto createRoleDto) {
        // Verificar si el rol ya existe
        if (roleRepository.existsByName(createRoleDto.getName())) {
            throw new IllegalStateException("El rol '" + createRoleDto.getName() + "' ya existe");
        }
        
        Role role = Role.builder()
                .name(createRoleDto.getName())
                .description(createRoleDto.getDescription())
                .permissions(createRoleDto.getPermissions() != null ? 
                    createRoleDto.getPermissions().toString() : null)
                .active(true)
                .build();
        
        Role savedRole = roleRepository.save(role);
        
        log.info("✅ Rol YEGO Principal creado: {}", savedRole.getName());
        
        return mapToResponseDto(savedRole);
    }
    
    @Override
    public List<RoleResponseDto> findAll() {
        List<Role> roles = roleRepository.findAllOrderByNameAsc();
        
        return roles.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public RoleResponseDto findOne(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol con ID " + id + " no encontrado"));
        
        return mapToResponseDto(role);
    }
    
    @Override
    public Role findByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Rol '" + name + "' no encontrado"));
    }
    
    @Override
    @Transactional
    public RoleResponseDto update(Long id, UpdateRoleDto updateRoleDto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol con ID " + id + " no encontrado"));
        
        // Verificar si el nuevo nombre ya existe (si se está cambiando)
        if (updateRoleDto.getName() != null && !updateRoleDto.getName().equals(role.getName())) {
            if (roleRepository.existsByName(updateRoleDto.getName())) {
                throw new IllegalStateException("El rol '" + updateRoleDto.getName() + "' ya existe");
            }
        }
        
        // Actualizar campos
        if (updateRoleDto.getName() != null) {
            role.setName(updateRoleDto.getName());
        }
        if (updateRoleDto.getDescription() != null) {
            role.setDescription(updateRoleDto.getDescription());
        }
        if (updateRoleDto.getPermissions() != null) {
            role.setPermissions(updateRoleDto.getPermissions().toString());
        }
        
        Role savedRole = roleRepository.save(role);
        
        log.info("✅ Rol YEGO Principal actualizado: {}", savedRole.getName());
        log.info("📝 Permisos guardados: {}", savedRole.getPermissions());
        
        // TODO: Emitir evento WebSocket a usuarios afectados
        // En una implementación completa, aquí se emitiría el evento 'permissions-updated'
        
        return mapToResponseDto(savedRole);
    }
    
    @Override
    @Transactional
    public void remove(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol con ID " + id + " no encontrado"));
        
        // Verificar si el rol tiene usuarios asignados
        Long userCount = getUserCountByRole(role.getName());
        if (userCount > 0) {
            throw new IllegalStateException("No se puede eliminar el rol '" + role.getName() + 
                    "' porque tiene " + userCount + " usuarios asignados");
        }
        
        roleRepository.delete(role);
        
        log.info("🗑️ Rol YEGO Principal eliminado: {}", role.getName());
    }
    
    @Override
    public List<RoleResponseDto> getDefaultRoles() {
        List<String> defaultRoleNames = Arrays.asList(
                "superadmin", "admin", "supervisor", "operador", "conductor", "agent"
        );
        
        List<Role> defaultRoles = roleRepository.findByNameIn(defaultRoleNames);
        
        return defaultRoles.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void initializeDefaultRoles() {
        List<DefaultRoleData> defaultRoles = getDefaultRolesData();
        
        for (DefaultRoleData defaultRole : defaultRoles) {
            if (!roleRepository.existsByName(defaultRole.name)) {
                CreateRoleDto createRoleDto = CreateRoleDto.builder()
                        .name(defaultRole.name)
                        .description(defaultRole.description)
                        .permissions(defaultRole.permissions)
                        .build();
                
                create(createRoleDto);
                log.info("⚙️ Rol por defecto YEGO Principal creado: {}", defaultRole.name);
            }
        }
    }
    
    @Override
    public Long getUserCountByRole(String roleName) {
        return userRepository.countActiveUsers(); // Simplificado por ahora
        // En una implementación completa sería:
        // return userRepository.countByRole(roleName);
    }
    
    private RoleResponseDto mapToResponseDto(Role role) {
        return RoleResponseDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(role.getPermissions())
                .active(role.getActive())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
    
    private List<DefaultRoleData> getDefaultRolesData() {
        return Arrays.asList(
                new DefaultRoleData("superadmin", "Super Administrador del Sistema", 
                        Map.of("all", true)),
                
                new DefaultRoleData("admin", "Administrador", 
                        Map.of(
                                "users", Arrays.asList("read", "write", "delete"),
                                "roles", Arrays.asList("read", "write"),
                                "modules", Arrays.asList("read", "write"),
                                "imports", Arrays.asList("read", "write"),
                                "audit", Arrays.asList("read"),
                                "configuration", Arrays.asList("read", "write")
                        )),
                
                new DefaultRoleData("supervisor", "Supervisor", 
                        Map.of(
                                "users", Arrays.asList("read"),
                                "imports", Arrays.asList("read", "write"),
                                "audit", Arrays.asList("read")
                        )),
                
                new DefaultRoleData("operador", "Operador", 
                        Map.of(
                                "imports", Arrays.asList("read", "write"),
                                "tickets", Arrays.asList("read", "write")
                        )),
                
                new DefaultRoleData("conductor", "Conductor", 
                        Map.of(
                                "profile", Arrays.asList("read", "write")
                        )),
                
                new DefaultRoleData("agent", "Agente de Soporte", 
                        Map.of(
                                "tickets", Arrays.asList("read", "write")
                        ))
        );
    }
    
    private static class DefaultRoleData {
        final String name;
        final String description;
        final Map<String, Object> permissions;
        
        DefaultRoleData(String name, String description, Map<String, Object> permissions) {
            this.name = name;
            this.description = description;
            this.permissions = permissions;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RoleResponseDto> findActive() {
        log.info("Obteniendo roles activos");
        List<Role> roles = roleRepository.findActiveRoles();
        return roles.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
}

