package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.Option;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import com.yego.backend.service.yego_ticketerera.OptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de Opciones del sistema YEGO Ticketerera
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptionServiceImpl implements OptionService {
    
    private final OptionRepository optionRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<Option> obtenerTodasLasOpciones() {
        log.info("Obteniendo todas las opciones activas");
        List<Option> options = optionRepository.findByActiveTrueOrderByPriorityAsc();
        log.info("Se encontraron {} opciones activas", options.size());
        return options;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Option> obtenerModulosActivos() {
        log.info("Obteniendo módulos activos (opciones principales)");
        List<Option> modules = optionRepository.findByParentIdIsNullAndActiveTrueOrderByPriorityAsc();
        log.info("Se encontraron {} módulos activos", modules.size());
        return modules;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Option> obtenerSubopciones(Long parentId) {
        log.info("Obteniendo subopciones para el módulo: {}", parentId);
        List<Option> suboptions = optionRepository.findByParentIdAndActiveTrueOrderByPriorityAsc(parentId);
        log.info("Se encontraron {} subopciones para el módulo {}", suboptions.size(), parentId);
        return suboptions;
    }
}

