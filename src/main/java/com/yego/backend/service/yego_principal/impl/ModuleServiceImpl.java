package com.yego.backend.service.yego_principal.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_principal.api.request.ModuleRequest;
import com.yego.backend.entity.yego_principal.api.response.GrupoResponse;
import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import com.yego.backend.entity.yego_principal.entities.Grupo;
import com.yego.backend.entity.yego_principal.entities.Module;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.GrupoRepository;
import com.yego.backend.repository.yego_principal.ModuleRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_principal.ModuleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final GrupoRepository grupoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> obtenerTodos() {
        log.info("📋 [ModuleService] Obteniendo todos los módulos");
        List<Module> modulos = moduleRepository.findAllByOrderByNombreAsc();
        log.info("✅ [ModuleService] Encontrados {} módulos", modulos.size());
        return modulos.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> obtenerActivos() {
        log.info("📋 [ModuleService] Obteniendo TODOS los módulos activos de la base de datos");
        List<Module> modulos = moduleRepository.findByActivoTrueOrderByNombreAsc();
        log.info("✅ [ModuleService] Total de módulos activos encontrados en BD: {}", modulos.size());
        
        if (modulos.isEmpty()) {
            log.warn("⚠️ [ModuleService] No hay módulos activos en la base de datos");
            return Collections.emptyList();
        }
        
        List<ModuleResponse> response = modulos.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
        
        log.info("✅ [ModuleService] Módulos convertidos a respuesta: {}", response.size());
        return response;
    }
    
    @Override
    @Transactional(readOnly = true)
    public ModuleResponse obtenerPorId(Long id) {
        log.info("📋 [ModuleService] Obteniendo módulo por ID: {}", id);
        Module modulo = moduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Módulo no encontrado con ID: " + id));
        log.info("✅ [ModuleService] Módulo encontrado: {}", modulo.getNombre());
        return convertirAResponse(modulo);
    }

    @Override
    @Transactional
    public ModuleResponse crear(ModuleRequest request) {
        log.info("📋 [ModuleService] Creando nuevo módulo: {}", request.getNombre());
        
        // Buscar grupo si se proporciona grupoId
        Grupo grupo = null;
        if (request.getGrupoId() != null) {
            grupo = grupoRepository.findById(request.getGrupoId())
                    .orElseThrow(() -> new EntityNotFoundException("Grupo no encontrado con ID: " + request.getGrupoId()));
            log.info("📋 [ModuleService] Grupo encontrado: {}", grupo.getNombre());
        }

        Module modulo = Module.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .url(request.getUrl())
                .codigo(normalizarCodigo(request.getCodigo()))
                .estado("activo")
                .icono(request.getIcono())
                .grupo(grupo)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .ultimoCheck(LocalDateTime.now()) // Actualizar ultimoCheck al crear
                .build();
        Module savedModulo = moduleRepository.save(modulo);
        log.info("✅ [ModuleService] Módulo creado con ID: {}", savedModulo.getId());
        return convertirAResponse(savedModulo);
    }

    @Override
    @Transactional
    public ModuleResponse actualizar(Long id, ModuleRequest request) {
        log.info("📋 [ModuleService] Actualizando módulo con ID: {}", id);
        Module modulo = moduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Módulo no encontrado con ID: " + id));

        // Buscar grupo si se proporciona grupoId
        Grupo grupo = null;
        if (request.getGrupoId() != null) {
            grupo = grupoRepository.findById(request.getGrupoId())
                    .orElseThrow(() -> new EntityNotFoundException("Grupo no encontrado con ID: " + request.getGrupoId()));
            log.info("📋 [ModuleService] Grupo encontrado: {}", grupo.getNombre());
        }

        modulo.setNombre(request.getNombre());
        modulo.setDescripcion(request.getDescripcion());
        modulo.setUrl(request.getUrl());
        if (request.getCodigo() != null) {
            modulo.setCodigo(normalizarCodigo(request.getCodigo()));
        }
        modulo.setEstado(request.getEstado() != null ? request.getEstado() : modulo.getEstado());
        modulo.setIcono(request.getIcono() != null ? request.getIcono() : modulo.getIcono());
        modulo.setGrupo(grupo);
        modulo.setActivo(request.getActivo() != null ? request.getActivo() : modulo.getActivo());
        modulo.setFechaActualizacion(LocalDateTime.now());
        modulo.setUltimoCheck(LocalDateTime.now()); // Actualizar ultimoCheck al actualizar

        Module updatedModulo = moduleRepository.save(modulo);
        log.info("✅ [ModuleService] Módulo actualizado con ID: {}", updatedModulo.getId());
        return convertirAResponse(updatedModulo);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("📋 [ModuleService] Eliminando módulo con ID: {}", id);
        Module modulo = moduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Módulo no encontrado con ID: " + id));
        moduleRepository.delete(modulo);
        log.info("✅ [ModuleService] Módulo eliminado con ID: {}", id);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        log.info("📋 [ModuleService] Toggle activo/inactivo módulo con ID: {}", id);
        Module modulo = moduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Módulo no encontrado con ID: " + id));
        
        // Cambiar el estado: si está activo lo desactiva, si está inactivo lo activa
        boolean nuevoEstado = !modulo.getActivo();
        modulo.setActivo(nuevoEstado);
        modulo.setEstado(nuevoEstado ? "activo" : "inactivo");
        modulo.setFechaActualizacion(LocalDateTime.now());
        modulo.setUltimoCheck(LocalDateTime.now()); // Actualizar ultimoCheck al hacer toggle
        moduleRepository.save(modulo);
        
        log.info("✅ [ModuleService] Módulo {} con ID: {}", 
                nuevoEstado ? "activado" : "desactivado", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> obtenerModulosPorUsuario(Long userId) {
        log.debug("📋 [ModuleService] Obteniendo módulos permitidos para usuario ID: {}", userId);
        
        // Obtener usuario con su rol
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));
        
        if (user.getRole() == null) {
            log.warn("⚠️ [ModuleService] Usuario {} no tiene rol asignado", userId);
            return Collections.emptyList();
        }
        
        String roleName = user.getRole().getName();

        if (roleName != null && "superadmin".equalsIgnoreCase(roleName.trim())) {
            log.info("🔑 [ModuleService] ════════════════════════════════════════════");
            log.info("🔑 [ModuleService] USUARIO ES SUPERADMIN (rol: {})", roleName);
            log.info("🔑 [ModuleService] Otorgando acceso completo a TODOS los módulos activos");
            log.info("🔑 [ModuleService] ════════════════════════════════════════════");
            
            List<ModuleResponse> allModules = obtenerActivos();
            
            log.info("✅ [ModuleService] SUPERADMIN - Total de módulos activos devueltos: {}", allModules.size());
            
            if (allModules.isEmpty()) {
                log.warn("⚠️ [ModuleService] ⚠️ ADVERTENCIA: No hay módulos activos en la base de datos para el superadmin");
            } else {
                log.info("✅ [ModuleService] Módulos disponibles para SUPERADMIN:");
                allModules.forEach(module -> 
                    log.info("   📦 ID: {} | Nombre: {} | URL: {} | Activo: {}", 
                        module.getId(), module.getNombre(), module.getUrl(), module.getActivo())
                );
            }
            
            log.info("🔑 [ModuleService] ════════════════════════════════════════════");
            return allModules;
        }
        
        // Obtener permisos del rol
        String permissionsJson = user.getRole().getPermissions();
        log.debug("📋 [ModuleService] JSON de permisos del rol {} (raw): {}", user.getRole().getName(), permissionsJson);
        
        if (permissionsJson == null || permissionsJson.isEmpty()) {
            log.warn("⚠️ [ModuleService] Rol {} no tiene permisos configurados", user.getRole().getName());
            return Collections.emptyList();
        }
        
        try {
            // Parsear JSON de permisos
            Map<String, Object> permissionsMap = objectMapper.readValue(
                    permissionsJson, 
                    new TypeReference<Map<String, Object>>() {}
            );
            
            log.debug("📋 [ModuleService] Permisos parseados para rol {}: {}", user.getRole().getName(), permissionsMap);
            
            // Si el rol tiene acceso completo a todos los módulos
            // Verificar primero si tiene la clave "all" y qué valor tiene
            boolean hasAllAccess = false;
            
            if (permissionsMap.containsKey("all")) {
                Object allPermission = permissionsMap.get("all");
                log.debug("📋 [ModuleService] ¿Contiene 'all'?: TRUE - Valor: {} (tipo: {})", 
                    allPermission, 
                    allPermission != null ? allPermission.getClass().getName() : "null");
                
                if (allPermission != null) {
                    if (allPermission instanceof Boolean) {
                        hasAllAccess = (Boolean) allPermission;
                        log.debug("📋 [ModuleService] 'all' es Boolean: {}", hasAllAccess);
                    } else if (allPermission instanceof String) {
                        String allStr = ((String) allPermission).toLowerCase().trim();
                        hasAllAccess = "true".equals(allStr) || "1".equals(allStr);
                        log.debug("📋 [ModuleService] 'all' es String '{}': {}", allStr, hasAllAccess);
                    } else if (allPermission instanceof Number) {
                        hasAllAccess = ((Number) allPermission).intValue() == 1;
                        log.debug("📋 [ModuleService] 'all' es Number {}: {}", allPermission, hasAllAccess);
                    }
                } else {
                    log.warn("⚠️ [ModuleService] 'all' existe pero es null");
                }
            } else {
                log.debug("📋 [ModuleService] ¿Contiene 'all'?: FALSE - El rol no tiene permiso 'all'");
            }
            
            if (hasAllAccess) {
                log.info("✅ [ModuleService] ════════════════════════════════════════════");
                log.info("✅ [ModuleService] Usuario {} (superadmin) tiene acceso completo (all=true)", userId);
                log.info("✅ [ModuleService] Devolviendo TODOS los módulos activos");
                log.info("✅ [ModuleService] ════════════════════════════════════════════");
                List<ModuleResponse> allModules = obtenerActivos();
                log.info("✅ [ModuleService] Total de módulos activos encontrados: {}", allModules.size());
                if (allModules.isEmpty()) {
                    log.warn("⚠️ [ModuleService] ⚠️ ADVERTENCIA: No hay módulos activos en la base de datos");
                } else {
                    log.info("✅ [ModuleService] Módulos que se devuelven: {}", 
                        allModules.stream().map(ModuleResponse::getNombre).collect(Collectors.toList()));
                }
                return allModules;
            }
            
            // Extraer nombres de módulos de las claves del mapa de permisos
            // Excluir "all" ya que es un permiso especial
            Set<String> moduleNamesFromPermissions = permissionsMap.keySet().stream()
                    .filter(key -> !"all".equalsIgnoreCase(key))
                    .collect(Collectors.toSet());
            
            log.debug("📋 [ModuleService] Permisos del rol {} (excluyendo 'all'): {}", user.getRole().getName(), moduleNamesFromPermissions);
            
            // Si no hay permisos específicos después de excluir "all", y no tiene acceso completo,
            // significa que el rol no tiene módulos asignados
            if (moduleNamesFromPermissions.isEmpty()) {
                log.warn("⚠️ [ModuleService] El rol {} no tiene módulos específicos asignados", user.getRole().getName());
                return Collections.emptyList();
            }
            
            // Obtener todos los módulos activos (ordenados por nombre)
            List<Module> allActiveModules = moduleRepository.findByActivoTrueOrderByNombreAsc();
            log.debug("📋 [ModuleService] Total de módulos activos en BD: {}", allActiveModules.size());
            
            // Crear un mapa de nombres normalizados de permisos para búsqueda rápida
            Set<String> normalizedPermissionKeys = moduleNamesFromPermissions.stream()
                    .map(this::normalizePermissionKey)
                    .collect(Collectors.toSet());
            
            log.debug("📋 [ModuleService] Claves de permisos normalizadas para buscar: {}", normalizedPermissionKeys);
            
            // Filtrar módulos que coincidan con los permisos
            // Buscamos coincidencias tanto en el nombre como en la URL del módulo
            List<Module> allowedModules = allActiveModules.stream()
                    .filter(module -> {
                        // Normalizar nombre del módulo
                        String normalizedModuleName = normalizeModuleName(module.getNombre());
                        
                        // Normalizar URL del módulo (puede contener el nombre del módulo)
                        String normalizedUrl = normalizeModuleName(module.getUrl());
                        
                        // Verificar si coincide con alguna clave de permiso
                        boolean matchesPermission = normalizedPermissionKeys.stream()
                                .anyMatch(permKey -> 
                                    normalizedModuleName.contains(permKey) || 
                                    permKey.contains(normalizedModuleName) ||
                                    normalizedUrl.contains(permKey) ||
                                    permKey.contains(normalizedUrl) ||
                                    normalizedModuleName.equalsIgnoreCase(permKey)
                                );
                        
                        // Si no hay coincidencia exacta pero el módulo tiene permisos configurados,
                        // podríamos devolverlo de todas formas. Por ahora, lo incluimos si hay algún match
                        return matchesPermission;
                    })
                    .collect(Collectors.toList());
            
            if (allowedModules.isEmpty() && !moduleNamesFromPermissions.isEmpty()) {
                log.warn("⚠️ [ModuleService] No se encontraron coincidencias para los permisos {} del rol {} (usuario {}). " +
                        "Verifica que los nombres de permisos coincidan con los módulos en BD.",
                        moduleNamesFromPermissions, user.getRole().getName(), userId);
            }
            
            log.debug("✅ [ModuleService] Encontrados {} módulos permitidos para usuario {}", allowedModules.size(), userId);
            
            return allowedModules.stream()
                    .map(this::convertirAResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("❌ [ModuleService] Error parseando permisos del rol: {}", e.getMessage());
            // Si hay error parseando, por seguridad devolvemos lista vacía
            return Collections.emptyList();
        }
    }
    
    /**
     * Normaliza el nombre del módulo para comparación
     */
    private String normalizeModuleName(String moduleName) {
        if (moduleName == null) return "";
        return moduleName.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }
    
    /**
     * Normaliza la clave de permiso para comparación
     */
    private String normalizePermissionKey(String permissionKey) {
        if (permissionKey == null) return "";
        return permissionKey.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return codigo.trim();
    }

    private ModuleResponse convertirAResponse(Module modulo) {
        // Normalizar la URL para asegurar que sea una ruta relativa del frontend
        String url = modulo.getUrl();
        
        // Si la URL es una URL completa (http:// o https://), extraer solo la ruta
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            try {
                java.net.URL urlObj = new java.net.URL(url);
                String path = urlObj.getPath();
                // Si la ruta está vacía, mantener la URL original
                if (path != null && !path.isEmpty()) {
                    url = path;
                    log.debug("📋 [ModuleService] URL normalizada: {} -> {}", modulo.getUrl(), url);
                }
            } catch (Exception e) {
                log.warn("⚠️ [ModuleService] Error normalizando URL {}: {}", url, e.getMessage());
            }
        }
        
        // Si la URL contiene /api/, es una URL de API y debe ser corregida
        // (no debería pasar, pero por seguridad)
        if (url != null && url.contains("/api/")) {
            log.warn("⚠️ [ModuleService] ADVERTENCIA: El módulo '{}' tiene una URL de API ({}) que no debería estar aquí", 
                modulo.getNombre(), url);
        }
        
        // Convertir grupo a GrupoResponse si existe
        GrupoResponse grupoResponse = null;
        if (modulo.getGrupo() != null) {
            Grupo grupo = modulo.getGrupo();
            grupoResponse = GrupoResponse.builder()
                    .id(grupo.getId())
                    .nombre(grupo.getNombre())
                    .icono(grupo.getIcono())
                    .activo(grupo.getActivo())
                    .fechaCreacion(grupo.getFechaCreacion())
                    .build();
        }
        
        return ModuleResponse.builder()
                .id(modulo.getId())
                .nombre(modulo.getNombre())
                .descripcion(modulo.getDescripcion())
                .url(url)
                .codigo(modulo.getCodigo())
                .estado(modulo.getEstado())
                .icono(modulo.getIcono())
                .grupo(grupoResponse)
                .ultimoCheck(modulo.getUltimoCheck())
                .activo(modulo.getActivo())
                .fechaCreacion(modulo.getFechaCreacion())
                .fechaActualizacion(modulo.getFechaActualizacion())
                .build();
    }
}
