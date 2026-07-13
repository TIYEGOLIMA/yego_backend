package com.yego.backend.service.yego_pro_ops.mobile;

import com.yego.backend.entity.yego_api_externo.entities.DriverApi;
import com.yego.backend.entity.yego_pro_ops.api.request.mobile.MobileShiftLocationRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.ShiftLocationResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.ShiftRouteResponse;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftLocationPoint;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.repository.yego_api_externo.DriverApiRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftLocationPointRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MobileShiftLocationService {

    private static final double MIN_DISTANCE_METERS = 50.0;
    private static final long MIN_SECONDS_BETWEEN_NEAR_POINTS = 120;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final ShiftSessionRepository shiftSessionRepository;
    private final ShiftLocationPointRepository locationRepository;
    private final DriverApiRepository driverApiRepository;

    @Transactional
    public ShiftLocationResponse saveLocation(String sessionId, MobileShiftLocationRequest request) {
        ShiftSession session = findSession(sessionId);
        if (!"active".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno no está activo");
        }

        Instant recordedAt = request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now();
        ShiftLocationPoint candidate = ShiftLocationPoint.builder()
                .shiftSessionId(session.getId())
                .driverId(session.getDriverId())
                .vehicleId(session.getVehicleId())
                .placa(session.getPlaca())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracy(request.getAccuracy())
                .speed(request.getSpeed())
                .heading(request.getHeading())
                .recordedAt(recordedAt)
                .source(firstNonBlank(request.getSource(), "mobile"))
                .build();

        ShiftLocationPoint saved = shouldPersist(candidate)
                ? locationRepository.save(candidate)
                : locationRepository.findFirstByShiftSessionIdOrderByRecordedAtDesc(session.getId()).orElse(candidate);

        return toLocationResponse(saved, session, findDriver(saved.getDriverId()));
    }

    @Transactional(readOnly = true)
    public List<ShiftLocationResponse> getActiveLocations() {
        List<ShiftLocationPoint> points = locationRepository.findLatestForActiveShifts();
        Map<UUID, ShiftSession> sessions = shiftSessionRepository.findByStatusAndDeletedFalseOrderByStartedAtDesc("active")
                .stream()
                .collect(Collectors.toMap(ShiftSession::getId, session -> session));
        Map<String, DriverApi> drivers = loadDrivers(points.stream().map(ShiftLocationPoint::getDriverId).toList());

        return points.stream()
                .map(point -> toLocationResponse(point, sessions.get(point.getShiftSessionId()), drivers.get(point.getDriverId())))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShiftRouteResponse getRoute(String sessionId) {
        ShiftSession session = findSession(sessionId);
        DriverApi driver = findDriver(session.getDriverId());
        List<ShiftLocationPoint> points = locationRepository.findByShiftSessionIdOrderByRecordedAtAsc(session.getId());
        List<ShiftLocationResponse> mapped = points.stream()
                .map(point -> toLocationResponse(point, session, driver))
                .toList();

        return ShiftRouteResponse.builder()
                .sessionId(session.getId().toString())
                .driverId(session.getDriverId())
                .driverName(resolveDriverName(driver))
                .vehicleId(session.getVehicleId())
                .placa(session.getPlaca())
                .startedAt(toInstant(session.getStartedAt()))
                .closedAt(toInstant(session.getClosedAt()))
                .status(session.getStatus())
                .points(mapped)
                .build();
    }

    @Transactional(readOnly = true)
    public ShiftLocationResponse getLastLocation(UUID sessionId) {
        return locationRepository.findFirstByShiftSessionIdOrderByRecordedAtDesc(sessionId)
                .map(point -> toLocationResponse(point, null, findDriver(point.getDriverId())))
                .orElse(null);
    }

    private boolean shouldPersist(ShiftLocationPoint candidate) {
        return locationRepository.findFirstByShiftSessionIdOrderByRecordedAtDesc(candidate.getShiftSessionId())
                .map(last -> isMeaningful(candidate, last))
                .orElse(true);
    }

    private boolean isMeaningful(ShiftLocationPoint candidate, ShiftLocationPoint last) {
        double distance = distanceMeters(candidate.getLatitude(), candidate.getLongitude(), last.getLatitude(), last.getLongitude());
        long seconds = Math.abs(Duration.between(last.getRecordedAt(), candidate.getRecordedAt()).getSeconds());
        return distance >= MIN_DISTANCE_METERS || seconds >= MIN_SECONDS_BETWEEN_NEAR_POINTS;
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private ShiftSession findSession(String id) {
        try {
            return shiftSessionRepository.findById(UUID.fromString(id))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado");
        }
    }

    private ShiftLocationResponse toLocationResponse(ShiftLocationPoint point, ShiftSession session, DriverApi driver) {
        if (point == null) return null;
        return ShiftLocationResponse.builder()
                .sessionId(point.getShiftSessionId().toString())
                .driverId(point.getDriverId())
                .driverName(resolveDriverName(driver))
                .vehicleId(firstNonBlank(point.getVehicleId(), session != null ? session.getVehicleId() : null))
                .placa(firstNonBlank(point.getPlaca(), session != null ? session.getPlaca() : null))
                .latitude(point.getLatitude())
                .longitude(point.getLongitude())
                .accuracy(point.getAccuracy())
                .speed(point.getSpeed())
                .heading(point.getHeading())
                .recordedAt(point.getRecordedAt())
                .source(point.getSource())
                .build();
    }

    private DriverApi findDriver(String driverId) {
        if (driverId == null || driverId.isBlank()) return null;
        return driverApiRepository.findById(driverId).orElse(null);
    }

    private Map<String, DriverApi> loadDrivers(List<String> driverIds) {
        List<String> ids = driverIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) return Map.of();
        return driverApiRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(DriverApi::getDriverId, driver -> driver, (first, ignored) -> first));
    }

    private String resolveDriverName(DriverApi driver) {
        if (driver == null) return null;
        return firstNonBlank(
                driver.getFullName(),
                (firstNonBlank(driver.getFirstName(), "") + " " + firstNonBlank(driver.getLastName(), "")).trim()
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private Instant toInstant(java.time.LocalDateTime value) {
        return value != null ? value.atZone(ZoneId.systemDefault()).toInstant() : null;
    }
}
