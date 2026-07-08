package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.request.OperationalTripFactInput;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalTripFactResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalShiftEvent;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalTripFact;
import com.yego.backend.repository.yego_pro_ops.OperationalShiftEventRepository;
import com.yego.backend.repository.yego_pro_ops.OperationalTripFactRepository;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.OperationalTripFactService;
import com.yego.backend.service.yego_pro_ops.OperationalVehicleKeyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OperationalTripFactServiceImpl implements OperationalTripFactService {

    private static final DateTimeFormatter YANGO_LOCAL_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final OperationalTripFactRepository tripFactRepository;
    private final OperationalShiftEventRepository shiftEventRepository;
    private final OperationalVehicleKeyResolver vehicleKeyResolver;
    private final DriverOrdersService driverOrdersService;
    private final OperationalDateRangeParser dateRangeParser;

    @Override
    @Transactional
    public OperationalTripFact upsertTripFact(OperationalTripFactInput input) {
        validateInput(input);

        OperationalVehicleKeyResolver.Resolution resolution = vehicleKeyResolver.resolve(input);
        OperationalTripFact tripFact = tripFactRepository.findByExternalTripId(input.getExternalTripId())
                .orElseGet(() -> OperationalTripFact.builder().externalTripId(input.getExternalTripId()).build());

        tripFact.setDriverId(input.getDriverId().trim());
        tripFact.setVehicleKey(resolution.vehicleKey());
        tripFact.setVehicleKeySource(resolution.vehicleKeySource());
        tripFact.setVehicleId(resolution.vehicleId());
        tripFact.setVehiclePlate(resolution.originalPlate());
        tripFact.setVehiclePlateNormalized(resolution.normalizedPlate());
        tripFact.setTripStatus(normalizeStatus(input.getTripStatus()));
        tripFact.setBookedAt(input.getBookedAt());
        tripFact.setEndedAt(input.getEndedAt());
        tripFact.setObservedAt(input.getObservedAt() != null ? input.getObservedAt() : dateRangeParser.now());
        tripFact.setSource(input.getSource() != null && !input.getSource().isBlank()
                ? input.getSource().trim()
                : OperationalMonitoringConstants.SOURCE_YANGO);
        tripFact.setSourcePayloadHash(input.getSourcePayloadHash());

        OperationalTripFact saved = tripFactRepository.save(tripFact);
        shiftEventRepository.save(OperationalShiftEvent.builder()
                .eventType(OperationalMonitoringConstants.EVENT_TRIP_FACT_UPSERTED)
                .eventTime(saved.getObservedAt())
                .driverId(saved.getDriverId())
                .vehicleKey(saved.getVehicleKey())
                .externalTripId(saved.getExternalTripId())
                .reason(saved.getVehicleKey() == null
                        ? OperationalMonitoringConstants.REASON_MISSING_VEHICLE_KEY
                        : "UPSERT")
                .details("Trip fact upserted in mirror mode")
                .build());
        return saved;
    }

    @Override
    @Transactional
    public List<OperationalTripFact> upsertTripFacts(List<OperationalTripFactInput> inputs) {
        List<OperationalTripFact> saved = new ArrayList<>();
        if (inputs == null) {
            return saved;
        }
        for (OperationalTripFactInput input : inputs) {
            saved.add(upsertTripFact(input));
        }
        return saved;
    }

    @Override
    @Transactional
    public List<OperationalTripFact> importFromDriverOrders(String driverId, String dateFrom, String dateTo) {
        DriverOrdersResponse response = driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo);
        List<OperationalTripFactInput> inputs = new ArrayList<>();
        LocalDateTime observedAt = dateRangeParser.now();
        if (response != null && response.getOrders() != null) {
            for (OrderInfoResponse order : response.getOrders()) {
                if (order == null || order.getId() == null || order.getId().isBlank()) {
                    continue;
                }
                inputs.add(OperationalTripFactInput.builder()
                        .externalTripId(order.getId())
                        .driverId(driverId)
                        .vehiclePlate(order.getCarLicenseNumber())
                        .tripStatus(order.getStatus())
                        .bookedAt(parseYangoLocalDateTime(order.getBookedAt()))
                        .endedAt(parseYangoLocalDateTime(order.getEndedAt()))
                        .observedAt(observedAt)
                        .source(OperationalMonitoringConstants.SOURCE_YANGO)
                        .sourcePayloadHash(hashSource(driverId, order))
                        .build());
            }
        }
        return upsertTripFacts(inputs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationalTripFactResponse> searchTripFacts(
            LocalDateTime from,
            LocalDateTime to,
            String driverId,
            String vehicleKey,
            String status,
            Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 200 : Math.min(limit, 1_000);
        return tripFactRepository.search(from, to, normalizeFilter(driverId), normalizeFilter(vehicleKey),
                        normalizeStatus(status), PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private OperationalTripFactResponse toResponse(OperationalTripFact fact) {
        return OperationalTripFactResponse.builder()
                .id(fact.getId())
                .externalTripId(fact.getExternalTripId())
                .driverId(fact.getDriverId())
                .vehicleKey(fact.getVehicleKey())
                .vehicleKeySource(fact.getVehicleKeySource())
                .vehicleId(fact.getVehicleId())
                .vehiclePlate(fact.getVehiclePlate())
                .vehiclePlateNormalized(fact.getVehiclePlateNormalized())
                .tripStatus(fact.getTripStatus())
                .bookedAt(fact.getBookedAt())
                .endedAt(fact.getEndedAt())
                .observedAt(fact.getObservedAt())
                .source(fact.getSource())
                .build();
    }

    private void validateInput(OperationalTripFactInput input) {
        if (input == null) {
            throw new IllegalArgumentException("OperationalTripFactInput is required");
        }
        if (input.getExternalTripId() == null || input.getExternalTripId().isBlank()) {
            throw new IllegalArgumentException("externalTripId is required");
        }
        if (input.getDriverId() == null || input.getDriverId().isBlank()) {
            throw new IllegalArgumentException("driverId is required");
        }
    }

    private String hashSource(String driverId, OrderInfoResponse order) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String seed = String.join("|",
                    driverId == null ? "" : driverId,
                    order.getId() == null ? "" : order.getId(),
                    order.getBookedAt() == null ? "" : order.getBookedAt(),
                    order.getEndedAt() == null ? "" : order.getEndedAt(),
                    order.getStatus() == null ? "" : order.getStatus(),
                    order.getCarLicenseNumber() == null ? "" : order.getCarLicenseNumber());
            return HexFormat.of().formatHex(digest.digest(seed.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private LocalDateTime parseYangoLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, YANGO_LOCAL_DATETIME);
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? null : status.trim().toLowerCase(Locale.ROOT);
    }
}
