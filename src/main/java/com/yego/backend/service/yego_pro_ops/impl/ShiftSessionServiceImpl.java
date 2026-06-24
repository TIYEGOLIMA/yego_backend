package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.response.ShiftSessionResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
        List<ShiftSession> sessions = shiftSessionRepository.findByDriverIdOrderByStartedAtDesc(driverId);

        Map<UUID, DriverClose> cierresPorSession = driverCloseRepository.findAllByDriverId(driverId)
                .stream()
                .filter(c -> c.getShiftSessionId() != null)
                .collect(Collectors.toMap(DriverClose::getShiftSessionId, c -> c, (a, b) -> a));

        return sessions.stream()
                .map(s -> {
                    ShiftSessionResponse resp = toShiftSessionResponse(s);
                    DriverClose cierre = cierresPorSession.get(s.getId());
                    if (cierre != null) {
                        resp.setLiquidaEfectivo(nz(cierre.getLiquidaEfectivo()));
                        resp.setLiquidaYape(nz(cierre.getLiquidaYape()));
                    }
                    return resp;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
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
        session.setStatus("por_validar");
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

        if (!"completada".equals(session.getStatus())) {
            throw new RuntimeException("Solo sesiones completadas pueden ser liquidadas. Estado actual: " + session.getStatus());
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
    public ShiftSessionResponse updateSessionStatus(UUID sessionId, String newStatus) {
        ShiftSession session = shiftSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada: " + sessionId));

        if (!"por_validar".equals(session.getStatus())) {
            throw new RuntimeException("Solo sesiones 'por validar' pueden cambiar estado. Estado actual: " + session.getStatus());
        }

        if (!List.of("completada", "rechazada").contains(newStatus)) {
            throw new RuntimeException("Estado inválido: " + newStatus + ". Estados permitidos: completada, rechazada");
        }

        session.setStatus(newStatus);
        session = shiftSessionRepository.save(session);

        log.info("[ShiftSessionService] estado actualizado sessionId={} nuevoEstado={}", sessionId, newStatus);

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

        // Si al eliminar este turno ya no quedan otras sesiones cerradas/liquidadas ese día,
        // borra cualquier cierre remanente de esa fecha para que no persista en liquidación.
        LocalDate fechaSesion = session.getStartedAt().toLocalDate();
        boolean quedanSesionesEseDia = shiftSessionRepository.findByDriverIdOrderByStartedAtDesc(session.getDriverId()).stream()
                .filter(otra -> !otra.getId().equals(sessionId))
                .filter(otra -> "por_validar".equals(otra.getStatus()) || "completada".equals(otra.getStatus()) || "settled".equals(otra.getStatus()))
                .anyMatch(otra -> otra.getStartedAt() != null && otra.getStartedAt().toLocalDate().equals(fechaSesion));
        if (!quedanSesionesEseDia) {
            driverCloseRepository.findFirstByDriverIdAndFechaOrderByIdDesc(session.getDriverId(), fechaSesion)
                    .ifPresent(driverCloseRepository::delete);
        }

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
                .totalCash(session.getTotalCash())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
