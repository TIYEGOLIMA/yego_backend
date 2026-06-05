package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.request.CloseShiftSessionRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.RegisterTripRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.SettleShiftSessionRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.RegisterTripResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.ShiftSessionResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.ShiftSessionSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.TripResponse;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.entity.yego_pro_ops.entities.Trip;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import com.yego.backend.repository.yego_pro_ops.TripRepository;
import com.yego.backend.service.yego_pro_ops.ShiftSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftSessionServiceImpl implements ShiftSessionService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ShiftSessionRepository shiftSessionRepository;
    private final TripRepository tripRepository;

    @Override
    @Transactional(readOnly = true)
    public ShiftSessionResponse getActiveSession(String driverId) {
        return shiftSessionRepository.findByDriverIdAndStatus(driverId, "active")
                .map(this::toShiftSessionResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftSessionSummaryResponse getSessionSummary(UUID sessionId) {
        ShiftSession session = shiftSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada: " + sessionId));

        List<Trip> trips = tripRepository.findByShiftSessionId(sessionId);

        LocalDateTime firstTrip = trips.stream()
                .map(Trip::getCompletedAt)
                .min(Comparator.naturalOrder())
                .orElse(null);

        LocalDateTime lastTrip = trips.stream()
                .map(Trip::getCompletedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        BigDecimal runningTotal = trips.stream()
                .map(Trip::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ShiftSessionSummaryResponse.builder()
                .id(session.getId())
                .driverId(session.getDriverId())
                .startedAt(session.getStartedAt())
                .closedAt(session.getClosedAt())
                .settledAt(session.getSettledAt())
                .status(session.getStatus())
                .totalTrips(session.getTotalTrips())
                .totalAmount(session.getTotalAmount())
                .tripCount((long) trips.size())
                .runningTotal(runningTotal)
                .firstTrip(firstTrip)
                .lastTrip(lastTrip)
                .createdAt(session.getCreatedAt())
                .build();
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
    @Transactional(readOnly = true)
    public List<TripResponse> getSessionTrips(UUID sessionId) {
        return tripRepository.findByShiftSessionId(sessionId)
                .stream()
                .map(this::toTripResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RegisterTripResponse registerTrip(RegisterTripRequest request) {
        String driverId = request.getDriverId();
        LocalDateTime completedAt = LocalDateTime.parse(request.getCompletedAt(), ISO_FORMATTER);

        Optional<ShiftSession> activeOpt = shiftSessionRepository.findByDriverIdAndStatus(driverId, "active");
        ShiftSession session;
        boolean sessionOpened;

        if (activeOpt.isEmpty()) {
            session = openSession(driverId, completedAt);
            sessionOpened = true;
            log.info("[ShiftSessionService] nueva sesión abierta automáticamente driverId={} sessionId={} startedAt={}",
                    driverId, session.getId(), session.getStartedAt());
        } else {
            session = activeOpt.get();
            sessionOpened = false;
        }

        Trip trip = Trip.builder()
                .driverId(driverId)
                .shiftSessionId(session.getId())
                .externalTripId(request.getExternalTripId())
                .completedAt(completedAt)
                .amount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
                .build();
        trip = tripRepository.save(trip);

        return RegisterTripResponse.builder()
                .trip(toTripResponse(trip))
                .sessionOpened(sessionOpened)
                .session(toShiftSessionResponse(session))
                .build();
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

    @Transactional
    protected ShiftSession openSession(String driverId, LocalDateTime tripCompletedAt) {
        Optional<ShiftSession> lastSettled = shiftSessionRepository
                .findTopByDriverIdAndStatusOrderByClosedAtDesc(driverId, "settled");

        LocalDateTime startedAt;
        if (lastSettled.isPresent()) {
            startedAt = lastSettled.get().getClosedAt();
        } else {
            startedAt = tripCompletedAt;
        }

        ShiftSession session = ShiftSession.builder()
                .driverId(driverId)
                .startedAt(startedAt)
                .status("active")
                .totalTrips(0)
                .totalAmount(BigDecimal.ZERO)
                .build();

        return shiftSessionRepository.save(session);
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

    private TripResponse toTripResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .driverId(trip.getDriverId())
                .shiftSessionId(trip.getShiftSessionId())
                .externalTripId(trip.getExternalTripId())
                .completedAt(trip.getCompletedAt())
                .amount(trip.getAmount())
                .createdAt(trip.getCreatedAt())
                .build();
    }
}
