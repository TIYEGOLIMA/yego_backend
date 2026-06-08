package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.response.ShiftSessionResponse;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.entity.yego_pro_ops.entities.Trip;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.FacturacionSemanalRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import com.yego.backend.repository.yego_pro_ops.TripRepository;
import com.yego.backend.service.yego_pro_ops.ShiftSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftSessionServiceImpl implements ShiftSessionService {

    private final ShiftSessionRepository shiftSessionRepository;
    private final TripRepository tripRepository;
    private final FacturacionSemanalRepository facturacionSemanalRepository;
    private final DriverCloseRepository driverCloseRepository;

    @Override
    @Transactional(readOnly = true)
    public ShiftSessionResponse getActiveSession(String driverId) {
        return shiftSessionRepository.findByDriverIdAndStatusAndDeletedFalse(driverId, "active")
                .map(this::toShiftSessionResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftSessionResponse> getDriverSessionHistory(String driverId) {
        return shiftSessionRepository.findByDriverIdOrderByStartedAtDesc(driverId)
                .stream()
                .map(this::toShiftSessionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShiftSessionResponse closeSession(UUID sessionId, Long closedBy) {
        ShiftSession session = shiftSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada: " + sessionId));

        if (!"active".equals(session.getStatus())) {
            throw new RuntimeException("Solo sesiones activas pueden ser cerradas. Estado actual: " + session.getStatus());
        }

        List<Trip> trips = tripRepository.findByShiftSessionId(sessionId);

        session.setClosedAt(LocalDateTime.now());
        session.setStatus("closed");
        session.setTotalTrips(trips.size());
        session.setTotalAmount(trips.stream()
                .map(Trip::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        session = shiftSessionRepository.save(session);

        log.info("[ShiftSessionService] sesión cerrada sessionId={} driverId={} totalTrips={} totalAmount={} closedBy={}",
                sessionId, session.getDriverId(), session.getTotalTrips(), session.getTotalAmount(), closedBy);

        return toShiftSessionResponse(session);
    }

    @Override
    @Transactional
    public ShiftSessionResponse settleSession(UUID sessionId, Long settledBy) {
        ShiftSession session = shiftSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada: " + sessionId));

        if (!"closed".equals(session.getStatus())) {
            throw new RuntimeException("Solo sesiones cerradas pueden ser liquidadas. Estado actual: " + session.getStatus());
        }

        session.setStatus("settled");
        session.setSettledAt(LocalDateTime.now());
        session = shiftSessionRepository.save(session);

        log.info("[ShiftSessionService] sesión liquidada sessionId={} driverId={} settledBy={}",
                sessionId, session.getDriverId(), settledBy);

        return toShiftSessionResponse(session);
    }

    @Override
    @Transactional
    public void eliminarSesion(UUID sessionId, Long userId, String reason) {
        if (!shiftSessionRepository.existsById(sessionId)) {
            throw new RuntimeException("Sesión no encontrada: " + sessionId);
        }

        ShiftSession session = shiftSessionRepository.findById(sessionId).get();
        if ("settled".equals(session.getStatus())) {
            facturacionSemanalRepository.deleteOverlappingWithDriver(
                    session.getDriverId(),
                    session.getStartedAt().toLocalDate(),
                    session.getClosedAt().toLocalDate());
        }

        driverCloseRepository.findFirstByShiftSessionIdOrderByIdDesc(sessionId)
                .ifPresent(driverCloseRepository::delete);

        List<Trip> trips = tripRepository.findByShiftSessionId(sessionId);
        if (!trips.isEmpty()) {
            tripRepository.deleteAll(trips);
        }

        shiftSessionRepository.delete(session);

        log.info("[ShiftSessionService] sesión eliminada sessionId={} userId={} reason={}", sessionId, userId, reason);
    }

    private ShiftSessionResponse toShiftSessionResponse(ShiftSession session) {
        return ShiftSessionResponse.builder()
                .id(session.getId())
                .driverId(session.getDriverId())
                .startedAt(session.getStartedAt())
                .closedAt(session.getClosedAt())
                .settledAt(session.getSettledAt())
                .status(session.getStatus())
                .totalTrips(session.getTotalTrips())
                .totalAmount(session.getTotalAmount())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
