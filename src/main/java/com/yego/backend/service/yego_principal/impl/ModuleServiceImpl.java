package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.ModuleRequest;
import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import com.yego.backend.entity.yego_principal.entities.Module;
import com.yego.backend.repository.yego_principal.ModuleRepository;
import com.yego.backend.service.yego_principal.ModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> obtenerTodos() {
        log.info("📋 [ModuleService] Obteniendo todos los módulos");
        List<Module> modulos = moduleRepository.findAll();
        log.info("✅ [ModuleService] Encontrados {} módulos", modulos.size());
        return modulos.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> obtenerActivos() {
        log.info("📋 [ModuleService] Obteniendo módulos activos");
        List<Module> modulos = moduleRepository.findByActivoTrue();
        log.info("✅ [ModuleService] Encontrados {} módulos activos", modulos.size());
        return modulos.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public ModuleResponse obtenerPorId(Long id) {
        log.info("📋 [ModuleService] Obteniendo módulo por ID: {}", id);
        Module modulo = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado con ID: " + id));
        log.info("✅ [ModuleService] Módulo encontrado: {}", modulo.getNombre());
        return convertirAResponse(modulo);
    }

    @Override
    @Transactional
    public ModuleResponse crear(ModuleRequest request) {
        log.info("📋 [ModuleService] Creando nuevo módulo: {}", request.getNombre());
        Module modulo = Module.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .url(request.getUrl())
                .estado("activo")
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
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado con ID: " + id));

        modulo.setNombre(request.getNombre());
        modulo.setDescripcion(request.getDescripcion());
        modulo.setUrl(request.getUrl());
        modulo.setEstado(request.getEstado() != null ? request.getEstado() : "inactivo");
        modulo.setActivo(request.getActivo());
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
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado con ID: " + id));
        moduleRepository.delete(modulo);
        log.info("✅ [ModuleService] Módulo eliminado con ID: {}", id);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        log.info("📋 [ModuleService] Toggle activo/inactivo módulo con ID: {}", id);
        Module modulo = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado con ID: " + id));
        
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

    private ModuleResponse convertirAResponse(Module modulo) {
        return ModuleResponse.builder()
                .id(modulo.getId())
                .nombre(modulo.getNombre())
                .descripcion(modulo.getDescripcion())
                .url(modulo.getUrl())
                .estado(modulo.getEstado())
                .ultimoCheck(modulo.getUltimoCheck())
                .activo(modulo.getActivo())
                .fechaCreacion(modulo.getFechaCreacion())
                .fechaActualizacion(modulo.getFechaActualizacion())
                .build();
    }
}
