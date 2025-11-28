package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.GrupoRequest;
import com.yego.backend.entity.yego_principal.api.response.GrupoResponse;
import com.yego.backend.entity.yego_principal.entities.Grupo;
import com.yego.backend.repository.yego_principal.GrupoRepository;
import com.yego.backend.service.yego_principal.GrupoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrupoServiceImpl implements GrupoService {

    private final GrupoRepository grupoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GrupoResponse> obtenerActivos() {
        log.info("📋 [GrupoService] Obteniendo grupos activos");
        List<Grupo> grupos = grupoRepository.findByActivoTrue();
        log.info("✅ [GrupoService] Encontrados {} grupos activos", grupos.size());
        
        if (grupos.isEmpty()) {
            log.warn("⚠️ [GrupoService] No hay grupos activos en la base de datos");
            return Collections.emptyList();
        }
        
        return grupos.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GrupoResponse crear(GrupoRequest request) {
        log.info("📋 [GrupoService] Creando nuevo grupo: {}", request.getNombre());
        
        // Verificar si ya existe un grupo con ese nombre
        if (grupoRepository.existsByNombre(request.getNombre())) {
            throw new RuntimeException("Ya existe un grupo con el nombre: " + request.getNombre());
        }
        
        Grupo grupo = Grupo.builder()
                .nombre(request.getNombre())
                .icono(request.getIcono())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .fechaCreacion(LocalDateTime.now())
                .build();
        
        Grupo savedGrupo = grupoRepository.save(grupo);
        log.info("✅ [GrupoService] Grupo creado con ID: {}", savedGrupo.getId());
        return convertirAResponse(savedGrupo);
    }

    @Override
    @Transactional
    public GrupoResponse actualizar(Long id, GrupoRequest request) {
        log.info("📋 [GrupoService] Actualizando grupo con ID: {}", id);
        Grupo grupo = grupoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado con ID: " + id));

        // Verificar si el nuevo nombre ya existe (y no es el mismo grupo)
        if (!grupo.getNombre().equals(request.getNombre()) && grupoRepository.existsByNombre(request.getNombre())) {
            throw new RuntimeException("Ya existe un grupo con el nombre: " + request.getNombre());
        }

        grupo.setNombre(request.getNombre());
        grupo.setIcono(request.getIcono());
        grupo.setActivo(request.getActivo() != null ? request.getActivo() : grupo.getActivo());

        Grupo updatedGrupo = grupoRepository.save(grupo);
        log.info("✅ [GrupoService] Grupo actualizado con ID: {}", updatedGrupo.getId());
        return convertirAResponse(updatedGrupo);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("📋 [GrupoService] Eliminando grupo con ID: {}", id);
        Grupo grupo = grupoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado con ID: " + id));
        grupoRepository.delete(grupo);
        log.info("✅ [GrupoService] Grupo eliminado con ID: {}", id);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        log.info("📋 [GrupoService] Toggle activo/inactivo grupo con ID: {}", id);
        Grupo grupo = grupoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado con ID: " + id));
        
        boolean nuevoEstado = !grupo.getActivo();
        grupo.setActivo(nuevoEstado);
        grupoRepository.save(grupo);
        
        log.info("✅ [GrupoService] Grupo {} con ID: {}", 
                nuevoEstado ? "activado" : "desactivado", id);
    }

    private GrupoResponse convertirAResponse(Grupo grupo) {
        return GrupoResponse.builder()
                .id(grupo.getId())
                .nombre(grupo.getNombre())
                .icono(grupo.getIcono())
                .activo(grupo.getActivo())
                .fechaCreacion(grupo.getFechaCreacion())
                .build();
    }
}

