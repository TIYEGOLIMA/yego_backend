package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.entity.yego_principal.entities.Role;
import com.yego.backend.repository.yego_principal.RoleRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_principal.RoleService;
import com.yego.backend.service.yego_principal.ModuleService;
import com.yego.backend.service.yego_principal.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.*;
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
    private final ObjectMapper objectMapper;
    private final ModuleService moduleService;
    private final PermissionService permissionService;
    
    @Override
    @Transactional
    public RoleResponseDto create(CreateRoleDto createRoleDto) {
        // Verificar si el rol ya existe
        if (roleRepository.existsByName(createRoleDto.getName())) {
            throw new IllegalStateException("El rol '" + createRoleDto.getName() + "' ya existe");
        }
        
        // Convertir permissions a JSON string
        String permissionsJson = null;
        if (createRoleDto.getPermissions() != null) {
            try {
                permissionsJson = objectMapper.writeValueAsString(createRoleDto.getPermissions());
            } catch (Exception e) {
                log.error("Error convirtiendo permissions a JSON: {}", e.getMessage());
                throw new RuntimeException("Error procesando permisos del rol");
            }
        }
        
        Role role = Role.builder()
                .name(createRoleDto.getName())
                .description(createRoleDto.getDescription())
                .permissions(permissionsJson)
                .activo(true)
                .build();
        
        Role savedRole = roleRepository.save(role);
        
        log.info("✅ Rol YEGO Principal creado: {}", savedRole.getName());
        
        return mapToResponseDto(savedRole);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RoleResponseDto> findAll() {
        List<Role> roles = roleRepository.findAllOrderByNameAsc();
        
        return roles.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RoleSimpleDto> findAllActive() {
        log.info("📋 [RoleService] Obteniendo roles activos (solo id y name)");
        List<Role> activeRoles = roleRepository.findActiveRoles();
        
        return activeRoles.stream()
                .map(this::mapToSimpleDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public RoleResponseDto findOne(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol con ID " + id + " no encontrado"));
        
        return mapToResponseDto(role);
    }
    
    @Override
    @Transactional(readOnly = true)
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
            try {
                String permissionsJson = objectMapper.writeValueAsString(updateRoleDto.getPermissions());
                role.setPermissions(permissionsJson);
            } catch (Exception e) {
                log.error("Error convirtiendo permissions a JSON en update: {}", e.getMessage());
                throw new RuntimeException("Error procesando permisos del rol");
            }
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
            // En lugar de eliminar, desactivar el rol
            role.setActivo(false);
            roleRepository.save(role);
            log.info("🔒 Rol YEGO Principal desactivado (tenía {} usuarios): {}", userCount, role.getName());
        } else {
            // Solo eliminar si no tiene usuarios asignados
            roleRepository.delete(role);
            log.info("🗑️ Rol YEGO Principal eliminado: {}", role.getName());
        }
    }
    
    @Override
    @Transactional
    public RoleResponseDto toggleStatus(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol con ID " + id + " no encontrado"));
        
        // Cambiar el estado activo - manejar null como false
        Boolean currentStatus = role.getActivo();
        boolean newStatus = currentStatus != null ? !currentStatus : true;
        role.setActivo(newStatus);
        
        Role savedRole = roleRepository.save(role);
        
        String status = newStatus ? "activado" : "desactivado";
        log.info("🔄 Rol YEGO Principal {}: {}", status, savedRole.getName());
        
        return mapToResponseDto(savedRole);
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
        Long userCount = userRepository.countByRoleName(role.getName());
        
        return RoleResponseDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(role.getPermissions())
                .active(role.getActivo())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .userCount(userCount)
                .build();
    }
    
    private RoleSimpleDto mapToSimpleDto(Role role) {
        return RoleSimpleDto.builder()
                .id(role.getId())
                .name(role.getName())
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
    
    @Override
    @Transactional(readOnly = true)
    public List<ModuleWithActionsDto> getModulesWithActions() {
        log.info("📋 [RoleService] Obteniendo módulos con acciones disponibles para crear/editar roles");
        
        // Obtener todos los módulos activos
        List<ModuleResponse> modules = moduleService.obtenerActivos();
        
        // Obtener todos los permisos activos
        List<PermissionResponseDto> permissions = permissionService.findAllActive();
        
        // Agrupar permisos por módulo
        Map<String, List<PermissionResponseDto>> permissionsByModule = permissions.stream()
                .collect(Collectors.groupingBy(PermissionResponseDto::getModule));
        
        log.info("📋 [RoleService] Módulos activos encontrados: {}", modules.size());
        log.info("📋 [RoleService] Permisos activos encontrados: {}", permissions.size());
        log.info("📋 [RoleService] Permisos agrupados por módulo: {}", permissionsByModule.keySet());
        
        // Mapear módulos con sus acciones
        List<ModuleWithActionsDto> modulesWithActions = modules.stream()
                .map(module -> {
                    // Normalizar nombre del módulo para matching con permissions.module
                    String moduleKey = normalizeModuleNameForMatching(module.getNombre());
                    
                    // También extraer clave desde la URL si contiene información útil
                    String urlKey = extractModuleKeyFromUrl(module.getUrl());
                    
                    log.debug("📋 [RoleService] Procesando módulo: {} (key: {}, urlKey: {})", 
                        module.getNombre(), moduleKey, urlKey);
                    
                    // Buscar acciones para este módulo
                    List<PermissionResponseDto> modulePermissions = permissionsByModule.entrySet().stream()
                            .filter(entry -> {
                                String permModule = entry.getKey().toLowerCase().trim();
                                String normalizedPermModule = normalizeModuleNameForMatching(permModule);
                                
                                // Comparar de múltiples formas
                                boolean matches = 
                                    normalizedPermModule.equalsIgnoreCase(moduleKey) || 
                                    moduleKey.equalsIgnoreCase(normalizedPermModule) ||
                                    moduleKey.contains(normalizedPermModule) ||
                                    normalizedPermModule.contains(moduleKey) ||
                                    (urlKey != null && !urlKey.isEmpty() && (
                                        normalizedPermModule.equalsIgnoreCase(urlKey) ||
                                        urlKey.contains(normalizedPermModule) ||
                                        normalizedPermModule.contains(urlKey)
                                    ));
                                
                                if (matches) {
                                    log.debug("✅ [RoleService] Match encontrado: módulo '{}' <-> permiso.module '{}'", 
                                        module.getNombre(), permModule);
                                }
                                
                                return matches;
                            })
                            .flatMap(entry -> entry.getValue().stream())
                            .collect(Collectors.toList());
                    
                    log.debug("📋 [RoleService] Módulo '{}' tiene {} permisos asociados", 
                        module.getNombre(), modulePermissions.size());
                    
                    // Convertir permisos a acciones (agrupar por acción para evitar duplicados)
                    List<ActionDto> actions = modulePermissions.stream()
                            .collect(Collectors.toMap(
                                PermissionResponseDto::getAction,
                                perm -> ActionDto.builder()
                                    .action(perm.getAction())
                                    .name(perm.getName())
                                    .description(perm.getDescription())
                                    .module(perm.getModule())
                                    .build(),
                                (existing, replacement) -> existing // Si hay duplicados, mantener el primero
                            ))
                            .values()
                            .stream()
                            .sorted((a1, a2) -> a1.getAction().compareToIgnoreCase(a2.getAction()))
                            .collect(Collectors.toList());
                    
                    return ModuleWithActionsDto.builder()
                            .id(module.getId())
                            .nombre(module.getNombre())
                            .descripcion(module.getDescripcion())
                            .url(module.getUrl())
                            .estado(module.getEstado())
                            .activo(module.getActivo())
                            .actions(actions)
                            .build();
                })
                .collect(Collectors.toList());
        
        log.info("✅ [RoleService] Devueltos {} módulos con acciones para crear/editar roles", modulesWithActions.size());
        return modulesWithActions;
    }
    
    /**
     * Normaliza el nombre del módulo para hacer matching con permissions.module
     */
    private String normalizeModuleNameForMatching(String moduleName) {
        if (moduleName == null) return "";
        return moduleName.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }
    
    /**
     * Extrae una clave de módulo desde la URL (útil para matching con permissions.module)
     */
    private String extractModuleKeyFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        
        try {
            // Si es una URL completa, extraer solo la ruta
            if (url.startsWith("http://") || url.startsWith("https://")) {
                java.net.URL urlObj = new java.net.URL(url);
                url = urlObj.getPath();
            }
            
            // Remover la barra inicial y extraer el primer segmento
            String path = url.startsWith("/") ? url.substring(1) : url;
            String[] parts = path.split("/");
            
            if (parts.length > 0 && !parts[0].isEmpty()) {
                return normalizeModuleNameForMatching(parts[0]);
            }
        } catch (Exception e) {
            log.debug("No se pudo extraer clave de URL: {}", url);
        }
        
        return null;
    }
}

