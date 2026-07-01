package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.FleetSegmentResponse;
import com.yego.backend.entity.yego_pro_ops.entities.FleetSegment;
import com.yego.backend.repository.yego_pro_ops.FleetSegmentRepository;
import com.yego.backend.repository.yego_pro_ops.FleetVehicleRepository;
import com.yego.backend.service.yego_garantizado.FlotaService;
import com.yego.backend.service.yego_pro_ops.FleetSegmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FleetSegmentServiceImpl implements FleetSegmentService {

    private final FleetSegmentRepository segmentRepository;
    private final FleetVehicleRepository vehicleRepository;
    private final FlotaService flotaService;

    @Override
    public List<FleetSegmentResponse> listarFlotas() {
        Map<String, FlotaResponse> partnersByPark = resolverPartners();

        return segmentRepository.findByActivoTrue().stream()
                .map(seg -> {
                    FlotaResponse partner = partnersByPark.get(seg.getParkId());
                    return FleetSegmentResponse.builder()
                            .id(seg.getId())
                            .parkId(seg.getParkId())
                            .nombre(partner != null ? partner.getName() : seg.getParkId())
                            .ciudad(partner != null ? partner.getCity() : null)
                            .activo(seg.getActivo())
                            .totalVehiculos(vehicleRepository.countBySegment_IdAndActivoTrue(seg.getId()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FleetSegmentResponse agregarFlota(String parkId, Long createdById) {
        if (parkId == null || parkId.isBlank()) {
            throw new IllegalArgumentException("El park_id es obligatorio");
        }
        String normalizado = parkId.trim();

        FleetSegment segment = segmentRepository.findByParkId(normalizado)
                .map(existing -> {
                    if (Boolean.FALSE.equals(existing.getActivo())) {
                        existing.setActivo(true);
                    }
                    return existing;
                })
                .orElseGet(() -> FleetSegment.builder()
                        .parkId(normalizado)
                        .activo(true)
                        .createdById(createdById)
                        .build());

        segment = segmentRepository.save(segment);

        Map<String, FlotaResponse> partnersByPark = resolverPartners();
        FlotaResponse partner = partnersByPark.get(segment.getParkId());

        return FleetSegmentResponse.builder()
                .id(segment.getId())
                .parkId(segment.getParkId())
                .nombre(partner != null ? partner.getName() : segment.getParkId())
                .ciudad(partner != null ? partner.getCity() : null)
                .activo(segment.getActivo())
                .totalVehiculos(vehicleRepository.countBySegment_IdAndActivoTrue(segment.getId()))
                .build();
    }

    @Override
    @Transactional
    public void desactivarFlota(UUID id) {
        segmentRepository.findById(id).ifPresent(seg -> {
            seg.setActivo(false);
            segmentRepository.save(seg);
        });
    }

    private Map<String, FlotaResponse> resolverPartners() {
        try {
            return flotaService.obtenerTodosLosPartners().stream()
                    .collect(Collectors.toMap(FlotaResponse::getId, Function.identity(), (a, b) -> a));
        } catch (Exception e) {
            log.warn("No se pudieron resolver nombres de partners: {}", e.getMessage());
            return Map.of();
        }
    }
}
