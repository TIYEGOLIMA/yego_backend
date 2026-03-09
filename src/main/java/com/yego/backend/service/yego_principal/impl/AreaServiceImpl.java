package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.CreateAreaDto;
import com.yego.backend.entity.yego_principal.api.request.UpdateAreaDto;
import com.yego.backend.entity.yego_principal.api.response.AreaResponseDto;
import com.yego.backend.entity.yego_principal.api.response.AreaSimpleDto;
import com.yego.backend.entity.yego_principal.api.response.ColaboradorDto;
import com.yego.backend.entity.yego_principal.api.response.UserSimpleDto;
import com.yego.backend.entity.yego_principal.entities.Area;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.repository.yego_principal.AreaRepository;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_principal.AreaService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AreaServiceImpl implements AreaService {

    private final AreaRepository areaRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AreaResponseDto create(CreateAreaDto dto) {
        if (areaRepository.existsByName(dto.getName())) {
            throw new IllegalStateException("El área '" + dto.getName() + "' ya existe");
        }
        Area area = Area.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .managerId(dto.getManagerId())
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .build();
        Area saved = areaRepository.save(area);
        log.info("Área creada: {}", saved.getName());
        return findOne(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaResponseDto> findAll() {
        List<Area> areas = areaRepository.findAllOrderByNameAsc();
        if (areas.isEmpty()) return Collections.emptyList();
        List<Long> managerIds = areas.stream().map(Area::getManagerId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> areaIds = areas.stream().map(Area::getId).collect(Collectors.toList());
        Map<Long, User> managerById = managerIds.isEmpty() ? Collections.emptyMap()
                : userRepository.findByIdInWithRole(managerIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Long> countByAreaId = areaIds.isEmpty() ? Collections.emptyMap()
                : userRepository.countByAreaIdIn(areaIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
        return areas.stream()
                .map(a -> mapToResponseDto(a, managerById, countByAreaId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaSimpleDto> findAllActive() {
        return areaRepository.findAllActivas().stream()
                .map(a -> AreaSimpleDto.builder().id(a.getId()).name(a.getName()).build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AreaResponseDto findOne(Long id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Área con ID " + id + " no encontrada"));
        return mapToResponseDto(area);
    }

    @Override
    @Transactional
    public AreaResponseDto update(Long id, UpdateAreaDto dto) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Área con ID " + id + " no encontrada"));
        if (dto.getName() != null && !dto.getName().equals(area.getName()) && areaRepository.existsByName(dto.getName())) {
            throw new IllegalStateException("El área '" + dto.getName() + "' ya existe");
        }
        if (dto.getName() != null) area.setName(dto.getName());
        if (dto.getDescription() != null) area.setDescription(dto.getDescription());
        if (dto.getActivo() != null) area.setActivo(dto.getActivo());
        if (dto.getManagerId() != null) {
            area.setManagerId(dto.getManagerId());
        }
        areaRepository.save(area);
        return findOne(id);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Área con ID " + id + " no encontrada"));
        Long count = userRepository.countByAreaId(id);
        if (count > 0) {
            area.setActivo(false);
            areaRepository.save(area);
            log.info("Área desactivada (tenía {} colaboradores): {}", count, area.getName());
        } else {
            areaRepository.delete(area);
            log.info("Área eliminada: {}", area.getName());
        }
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        int updated = areaRepository.toggleActivoById(id);
        if (updated == 0) {
            throw new EntityNotFoundException("Área con ID " + id + " no encontrada");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSimpleDto> findUsersForResponsable(Long areaIdEnEdicion) {
        return userRepository.findActiveUsersForResponsableDropdown().stream()
                .map(row -> {
                    Long id = ((Number) row[0]).longValue();
                    String name = row[1] != null ? (String) row[1] : "";
                    String lastName = row[2] != null ? (String) row[2] : "";
                    String nombreCompleto = (name + " " + lastName).trim();
                    return UserSimpleDto.builder().id(id).nombreCompleto(nombreCompleto).areaId(null).build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ColaboradorDto> getColaboradoresByAreaId(Long areaId) {
        return userRepository.findColaboradoresProjectionByAreaId(areaId).stream()
                .map(row -> {
                    Long id = ((Number) row[0]).longValue();
                    String name = row[1] != null ? (String) row[1] : "";
                    String lastName = row[2] != null ? (String) row[2] : "";
                    String email = row[3] != null ? (String) row[3] : "";
                    String rol = row[4] != null ? (String) row[4] : "";
                    String nombreCompleto = (name + " " + lastName).trim();
                    return ColaboradorDto.builder()
                            .id(id)
                            .nombreCompleto(nombreCompleto)
                            .email(email)
                            .rol(rol)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /** Usa mapas pre-cargados para evitar N+1. */
    private AreaResponseDto mapToResponseDto(Area area, Map<Long, User> managerById, Map<Long, Long> countByAreaId) {
        String managerName = null;
        if (area.getManagerId() != null) {
            User manager = managerById.get(area.getManagerId());
            if (manager != null) {
                managerName = ((manager.getName() != null ? manager.getName() : "") + " " + (manager.getLastName() != null ? manager.getLastName() : "")).trim();
            }
        }
        Long colaboradoresCount = countByAreaId.getOrDefault(area.getId(), 0L);
        return AreaResponseDto.builder()
                .id(area.getId())
                .name(area.getName())
                .description(area.getDescription())
                .managerId(area.getManagerId())
                .managerName(managerName)
                .activo(area.getActivo())
                .colaboradoresCount(colaboradoresCount)
                .createdAt(area.getCreatedAt())
                .updatedAt(area.getUpdatedAt())
                .build();
    }

    private AreaResponseDto mapToResponseDto(Area area) {
        List<Long> managerIds = area.getManagerId() != null ? List.of(area.getManagerId()) : Collections.emptyList();
        Map<Long, User> managerById = managerIds.isEmpty() ? Collections.emptyMap()
                : userRepository.findByIdInWithRole(managerIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Long> countByAreaId = userRepository.countByAreaIdIn(List.of(area.getId())).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
        return mapToResponseDto(area, managerById, countByAreaId);
    }
}
