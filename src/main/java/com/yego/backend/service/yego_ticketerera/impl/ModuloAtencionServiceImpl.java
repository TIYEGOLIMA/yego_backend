package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.repository.yego_ticketerera.ModuloAtencionRepository;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de Módulos de Atención del sistema YEGO Ticketerera
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModuloAtencionServiceImpl implements ModuloAtencionService {
    
    private final ModuloAtencionRepository moduloAtencionRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<ModuloAtencion> obtenerTodosLosModulosActivos() {
        log.info("Obteniendo todos los módulos de atención activos");
        List<ModuloAtencion> modules = moduloAtencionRepository.findByIsActiveTrueOrderByNameAsc();
        log.info("Se encontraron {} módulos de atención activos", modules.size());
        return modules;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ModuloAtencion> obtenerTodosLosModulos() {
        log.info("Obteniendo todos los módulos de atención");
        return moduloAtencionRepository.findAll();
    }
    
    @Override
    @Transactional
    public void cambiarEstadoModulo(Long moduleId, boolean activo) {
        log.info("Cambiando estado del módulo {} a: {}", moduleId, activo ? "ACTIVO" : "INACTIVO");
        
        Optional<ModuloAtencion> moduloOpt = moduloAtencionRepository.findById(moduleId);
        if (moduloOpt.isPresent()) {
            ModuloAtencion modulo = moduloOpt.get();
            modulo.setIsActive(activo);
            moduloAtencionRepository.save(modulo);
            
            log.info("Estado del módulo {} cambiado a: {}", moduleId, activo ? "ACTIVO" : "INACTIVO");
        } else {
            log.warn("Módulo {} no encontrado", moduleId);
            throw new IllegalArgumentException("Módulo no encontrado con ID: " + moduleId);
        }
    }
    
    // Método simplificado para el controlador (sin lógica de negocio en el controlador)
    
    @Override
    @Transactional(readOnly = true)
    public List<ModuloAtencionResponse> obtenerModulosParaFrontend() {
        log.info("Obteniendo módulos de atención para el frontend");
        List<ModuloAtencion> modules = obtenerTodosLosModulosActivos();
        
        List<ModuloAtencionResponse> responses = modules.stream()
                .map(ModuloAtencionResponse::fromModuloAtencion)
                .toList();
        
        log.info("Se enviaron {} módulos de atención al frontend", responses.size());
        return responses;
    }
}
