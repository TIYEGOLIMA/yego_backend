package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearSedeRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.SedeResponse;
import com.yego.backend.entity.yego_ticketerera.entities.Sede;
import com.yego.backend.repository.yego_ticketerera.SedeRepository;
import com.yego.backend.service.yego_ticketerera.SedeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SedeServiceImpl implements SedeService {

    private final SedeRepository sedeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SedeResponse> listarSedes() {
        return sedeRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(SedeResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SedeResponse obtenerSede(Long id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + id));
        return SedeResponse.from(sede);
    }

    @Override
    @Transactional
    public SedeResponse crearSede(CrearSedeRequest request) {
        if (sedeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Ya existe una sede con el nombre: " + request.getName());
        }
        Sede sede = Sede.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .build();
        Sede saved = sedeRepository.save(sede);
        log.info("[Sede] Creada: {} (id={})", saved.getName(), saved.getId());
        return SedeResponse.from(saved);
    }

    @Override
    @Transactional
    public SedeResponse actualizarSede(Long id, CrearSedeRequest request) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + id));
        sede.setName(request.getName());
        sede.setDescription(request.getDescription());
        Sede saved = sedeRepository.save(sede);
        log.info("[Sede] Actualizada: {} (id={})", saved.getName(), saved.getId());
        return SedeResponse.from(saved);
    }

    @Override
    @Transactional
    public void desactivarSede(Long id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + id));
        sede.setActive(false);
        sedeRepository.save(sede);
        log.info("[Sede] Desactivada id={}", id);
    }
}
