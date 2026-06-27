package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.repository.yego_pro_ops.OperationalValidationDriverCloseReadRepository;
import com.yego.backend.repository.yego_pro_ops.OperationalValidationShiftSessionReadRepository;
import com.yego.backend.service.yego_pro_ops.OperationalManualShiftReadAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OperationalManualShiftReadAdapterImpl implements OperationalManualShiftReadAdapter {

    private final OperationalValidationShiftSessionReadRepository shiftSessionReadRepository;
    private final OperationalValidationDriverCloseReadRepository driverCloseReadRepository;

    @Override
    public List<ManualShiftSnapshot> findManualShifts(LocalDateTime from, LocalDateTime to, String driverId, String vehicleKey) {
        List<ShiftSession> sessions = shiftSessionReadRepository.findForValidation(from, to, normalize(driverId));
        Map<UUID, DriverClose> closesByShiftId = driverCloseReadRepository.findByShiftSessionIdIn(
                        sessions.stream().map(ShiftSession::getId).filter(Objects::nonNull).toList())
                .stream()
                .filter(close -> close.getShiftSessionId() != null)
                .collect(Collectors.toMap(DriverClose::getShiftSessionId, Function.identity(), (left, right) -> left));

        String normalizedVehicleKey = normalizePlate(vehicleKey);
        return sessions.stream()
                .map(session -> {
                    DriverClose close = closesByShiftId.get(session.getId());
                    return new ManualShiftSnapshot(
                            session.getId(),
                            session.getDriverId(),
                            session.getStartedAt(),
                            session.getClosedAt(),
                            session.getStatus(),
                            normalizePlate(close != null ? close.getPlaca() : null));
                })
                .filter(snapshot -> normalizedVehicleKey == null
                        || normalizedVehicleKey.equals(snapshot.plateNormalized()))
                .toList();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizePlate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replaceAll("[\\s\\-_/.:]", "");
        return normalized.isBlank() ? null : normalized;
    }
}
