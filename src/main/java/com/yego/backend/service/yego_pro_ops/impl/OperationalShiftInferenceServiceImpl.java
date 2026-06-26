package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringProperties;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalShiftEventResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalShiftSessionResponse;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalShiftEvent;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalShiftSession;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalTripFact;
import com.yego.backend.repository.yego_pro_ops.OperationalShiftEventRepository;
import com.yego.backend.repository.yego_pro_ops.OperationalShiftSessionRepository;
import com.yego.backend.repository.yego_pro_ops.OperationalTripFactRepository;
import com.yego.backend.service.yego_pro_ops.OperationalShiftInferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperationalShiftInferenceServiceImpl implements OperationalShiftInferenceService {

    private final OperationalTripFactRepository tripFactRepository;
    private final OperationalShiftSessionRepository shiftSessionRepository;
    private final OperationalShiftEventRepository shiftEventRepository;
    private final OperationalMonitoringProperties properties;

    @Override
    @Transactional
    public ReprocessResult reprocessRange(LocalDateTime from, LocalDateTime to, String driverId, String vehicleKey) {
        List<OperationalTripFact> facts = tripFactRepository.findForInference(from, to, normalize(driverId), normalize(vehicleKey))
                .stream()
                .filter(this::isCompletedStatus)
                .sorted(Comparator
                        .comparing((OperationalTripFact fact) -> normalizeVehicleGroupKey(fact.getVehicleKey()), Comparator.nullsLast(String::compareTo))
                        .thenComparing(this::getOperationalTimestamp, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(OperationalTripFact::getExternalTripId, Comparator.nullsLast(String::compareTo)))
                .toList();

        clearPreviousInference(from, to, driverId, vehicleKey);

        List<OperationalShiftSession> sessionsToSave = new ArrayList<>();
        List<OperationalShiftEvent> eventsToSave = new ArrayList<>();

        Map<String, List<OperationalTripFact>> byVehicleKey = new LinkedHashMap<>();
        List<OperationalTripFact> unresolvedFacts = new ArrayList<>();

        for (OperationalTripFact fact : facts) {
            if (fact.getVehicleKey() == null || fact.getVehicleKey().isBlank()) {
                unresolvedFacts.add(fact);
            } else {
                byVehicleKey.computeIfAbsent(fact.getVehicleKey(), key -> new ArrayList<>()).add(fact);
            }
        }

        for (List<OperationalTripFact> vehicleFacts : byVehicleKey.values()) {
            inferVehicleSessions(vehicleFacts, to, sessionsToSave, eventsToSave);
        }
        inferUnresolvedSessions(unresolvedFacts, sessionsToSave, eventsToSave);

        List<OperationalShiftSession> savedSessions = shiftSessionRepository.saveAll(sessionsToSave);
        attachSessionIds(savedSessions, eventsToSave);
        List<OperationalShiftEvent> savedEvents = shiftEventRepository.saveAll(eventsToSave);

        return new ReprocessResult(facts.size(), savedSessions.size(), savedEvents.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationalShiftSessionResponse> searchShifts(
            LocalDateTime from,
            LocalDateTime to,
            String driverId,
            String vehicleKey,
            String state) {
        return shiftSessionRepository.search(from, to, normalize(driverId), normalize(vehicleKey), normalize(state))
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationalShiftEventResponse> searchEvents(
            LocalDateTime from,
            LocalDateTime to,
            UUID shiftId,
            String driverId,
            String vehicleKey) {
        return shiftEventRepository.search(from, to, shiftId, normalize(driverId), normalize(vehicleKey))
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    private void clearPreviousInference(LocalDateTime from, LocalDateTime to, String driverId, String vehicleKey) {
        List<OperationalShiftSession> existingSessions = shiftSessionRepository.findForReprocess(from, to, normalize(driverId), normalize(vehicleKey));
        if (!existingSessions.isEmpty()) {
            List<UUID> shiftIds = existingSessions.stream().map(OperationalShiftSession::getId).toList();
            shiftEventRepository.deleteByOperationalShiftSessionIdIn(shiftIds);
            shiftSessionRepository.deleteAll(existingSessions);
        }
        shiftEventRepository.deleteStandaloneInferenceEvents(from, to, normalize(driverId), normalize(vehicleKey));
    }

    private void inferVehicleSessions(
            List<OperationalTripFact> vehicleFacts,
            LocalDateTime rangeEnd,
            List<OperationalShiftSession> sessionsToSave,
            List<OperationalShiftEvent> eventsToSave) {
        OperationalShiftSession currentSession = null;

        for (OperationalTripFact fact : vehicleFacts) {
            LocalDateTime timestamp = getOperationalTimestamp(fact);
            if (timestamp == null) {
                continue;
            }
            if (currentSession == null) {
                currentSession = openSession(fact, timestamp, false);
                sessionsToSave.add(currentSession);
                eventsToSave.add(buildEvent(null, OperationalMonitoringConstants.EVENT_SHIFT_OPENED, timestamp, fact,
                        OperationalMonitoringConstants.REASON_FIRST_COMPLETED_TRIP, "Opened from first completed trip"));
                maybeRegisterFallbackEvent(currentSession, fact, timestamp, eventsToSave);
                continue;
            }

            if (timestamp.isBefore(currentSession.getOpenedAt()) || timestamp.isBefore(currentSession.getLastActivityAt())) {
                currentSession.setNeedsReview(true);
                currentSession.setReviewReason(OperationalMonitoringConstants.EVENT_LATE_TRIP_DETECTED);
                currentSession.setState(OperationalMonitoringConstants.STATE_NEEDS_REVIEW);
                eventsToSave.add(buildEvent(null, OperationalMonitoringConstants.EVENT_LATE_TRIP_DETECTED, timestamp, fact,
                        OperationalMonitoringConstants.EVENT_LATE_TRIP_DETECTED, "Trip arrived before current inferred timeline"));
            }

            if (currentSession.getDriverId().equals(fact.getDriverId())) {
                currentSession.setTripCount(currentSession.getTripCount() + 1);
                currentSession.setLastTripExternalId(fact.getExternalTripId());
                currentSession.setLastActivityAt(timestamp);
                currentSession.setConfidenceLevel(downgradeConfidenceIfNeeded(currentSession.getConfidenceLevel(), fact));
                eventsToSave.add(buildEvent(null, OperationalMonitoringConstants.EVENT_SHIFT_CONTINUED, timestamp, fact,
                        OperationalMonitoringConstants.REASON_SAME_DRIVER_ACTIVITY, "Shift continued by same driver"));
                maybeRegisterFallbackEvent(currentSession, fact, timestamp, eventsToSave);
                continue;
            }

            currentSession.setClosedAt(timestamp);
            currentSession.setState(OperationalMonitoringConstants.STATE_AUTO_CLOSED_BY_NEXT_DRIVER);
            currentSession.setCloseReason(OperationalMonitoringConstants.REASON_NEXT_DRIVER_ACTIVITY);
            currentSession.setLastActivityAt(timestamp);
            eventsToSave.add(buildEvent(null, OperationalMonitoringConstants.EVENT_SHIFT_AUTO_CLOSED_BY_NEXT_DRIVER, timestamp, fact,
                    OperationalMonitoringConstants.REASON_NEXT_DRIVER_ACTIVITY, "Shift auto-closed by next driver activity"));

            currentSession = openSession(fact, timestamp, false);
            sessionsToSave.add(currentSession);
            eventsToSave.add(buildEvent(null, OperationalMonitoringConstants.EVENT_SHIFT_OPENED, timestamp, fact,
                    OperationalMonitoringConstants.REASON_FIRST_COMPLETED_TRIP, "Opened after driver switch"));
            maybeRegisterFallbackEvent(currentSession, fact, timestamp, eventsToSave);
        }

        if (currentSession != null && currentSession.getClosedAt() == null && shouldMarkStale(currentSession, rangeEnd)) {
            currentSession.setState(OperationalMonitoringConstants.STATE_STALE_CANDIDATE);
            currentSession.setNeedsReview(false);
            currentSession.setCloseReason(OperationalMonitoringConstants.REASON_STALE_THRESHOLD_EXCEEDED);
            eventsToSave.add(OperationalShiftEvent.builder()
                    .eventType(OperationalMonitoringConstants.EVENT_SHIFT_MARKED_STALE_CANDIDATE)
                    .eventTime(currentSession.getLastActivityAt())
                    .driverId(currentSession.getDriverId())
                    .vehicleKey(currentSession.getVehicleKey())
                    .externalTripId(currentSession.getLastTripExternalId())
                    .reason(OperationalMonitoringConstants.REASON_STALE_THRESHOLD_EXCEEDED)
                    .details("Open inferred shift exceeded stale threshold")
                    .build());
        }
    }

    private void inferUnresolvedSessions(
            List<OperationalTripFact> unresolvedFacts,
            List<OperationalShiftSession> sessionsToSave,
            List<OperationalShiftEvent> eventsToSave) {
        Map<String, OperationalShiftSession> sessionsByDriver = new HashMap<>();
        unresolvedFacts.stream()
                .sorted(Comparator.comparing(this::getOperationalTimestamp, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(OperationalTripFact::getExternalTripId, Comparator.nullsLast(String::compareTo)))
                .forEach(fact -> {
                    LocalDateTime timestamp = getOperationalTimestamp(fact);
                    if (timestamp == null) {
                        return;
                    }
                    OperationalShiftSession session = sessionsByDriver.computeIfAbsent(fact.getDriverId(), key -> {
                        OperationalShiftSession created = openSession(fact, timestamp, true);
                        sessionsToSave.add(created);
                        return created;
                    });
                    if (!session.getFirstTripExternalId().equals(fact.getExternalTripId())) {
                        session.setTripCount(session.getTripCount() + 1);
                    }
                    session.setLastTripExternalId(fact.getExternalTripId());
                    session.setLastActivityAt(timestamp);
                    session.setNeedsReview(true);
                    session.setState(OperationalMonitoringConstants.STATE_NEEDS_REVIEW);
                    session.setReviewReason(OperationalMonitoringConstants.REASON_MISSING_VEHICLE_KEY);
                    eventsToSave.add(buildEvent(null, OperationalMonitoringConstants.EVENT_MISSING_VEHICLE_KEY, timestamp, fact,
                            OperationalMonitoringConstants.REASON_MISSING_VEHICLE_KEY, "Trip fact missing vehicle key"));
                    eventsToSave.add(buildEvent(null, OperationalMonitoringConstants.EVENT_SHIFT_NEEDS_REVIEW, timestamp, fact,
                            OperationalMonitoringConstants.REASON_MISSING_VEHICLE_KEY, "Shift marked for review because vehicle key is unresolved"));
                    maybeRegisterFallbackEvent(session, fact, timestamp, eventsToSave);
                });
    }

    private void attachSessionIds(List<OperationalShiftSession> savedSessions, List<OperationalShiftEvent> eventsToSave) {
        Map<String, OperationalShiftSession> shiftIndex = new HashMap<>();
        for (OperationalShiftSession session : savedSessions) {
            String key = sessionIdentity(session.getDriverId(), session.getVehicleKey(), session.getOpenedAt(), session.getFirstTripExternalId());
            shiftIndex.put(key, session);
        }
        for (OperationalShiftEvent event : eventsToSave) {
            if (event.getOperationalShiftSessionId() != null) {
                continue;
            }
            String key = sessionIdentity(event.getDriverId(), event.getVehicleKey(), event.getEventTime(), event.getExternalTripId());
            OperationalShiftSession directMatch = shiftIndex.get(key);
            if (directMatch != null) {
                event.setOperationalShiftSessionId(directMatch.getId());
                continue;
            }

            for (OperationalShiftSession session : savedSessions) {
                if (!safeEquals(session.getDriverId(), event.getDriverId())) {
                    continue;
                }
                if (!safeEquals(session.getVehicleKey(), event.getVehicleKey())) {
                    continue;
                }
                if (session.getOpenedAt() != null && event.getEventTime() != null && event.getEventTime().isBefore(session.getOpenedAt())) {
                    continue;
                }
                if (session.getClosedAt() != null && event.getEventTime() != null && event.getEventTime().isAfter(session.getClosedAt())) {
                    continue;
                }
                event.setOperationalShiftSessionId(session.getId());
                break;
            }
        }
    }

    private String sessionIdentity(String driverId, String vehicleKey, LocalDateTime openedAt, String firstTripId) {
        return String.join("|",
                driverId == null ? "" : driverId,
                vehicleKey == null ? "" : vehicleKey,
                openedAt == null ? "" : openedAt.toString(),
                firstTripId == null ? "" : firstTripId);
    }

    private OperationalShiftSession openSession(OperationalTripFact fact, LocalDateTime timestamp, boolean needsReview) {
        String state = needsReview
                ? OperationalMonitoringConstants.STATE_NEEDS_REVIEW
                : OperationalMonitoringConstants.STATE_OPEN_INFERRED;
        return OperationalShiftSession.builder()
                .driverId(fact.getDriverId())
                .vehicleKey(needsReview ? null : fact.getVehicleKey())
                .vehicleKeySource(fact.getVehicleKeySource())
                .vehicleId(fact.getVehicleId())
                .vehiclePlateNormalized(fact.getVehiclePlateNormalized())
                .openedAt(timestamp)
                .state(state)
                .openReason(OperationalMonitoringConstants.REASON_FIRST_COMPLETED_TRIP)
                .firstTripExternalId(fact.getExternalTripId())
                .lastTripExternalId(fact.getExternalTripId())
                .tripCount(1)
                .lastActivityAt(timestamp)
                .confidenceLevel(determineConfidence(fact))
                .needsReview(needsReview)
                .reviewReason(needsReview ? OperationalMonitoringConstants.REASON_MISSING_VEHICLE_KEY : null)
                .build();
    }

    private OperationalShiftEvent buildEvent(
            UUID shiftId,
            String eventType,
            LocalDateTime eventTime,
            OperationalTripFact fact,
            String reason,
            String details) {
        return OperationalShiftEvent.builder()
                .operationalShiftSessionId(shiftId)
                .eventType(eventType)
                .eventTime(eventTime)
                .driverId(fact.getDriverId())
                .vehicleKey(fact.getVehicleKey())
                .externalTripId(fact.getExternalTripId())
                .reason(reason)
                .details(details)
                .build();
    }

    private void maybeRegisterFallbackEvent(
            OperationalShiftSession session,
            OperationalTripFact fact,
            LocalDateTime timestamp,
            List<OperationalShiftEvent> eventsToSave) {
        if (fact.getBookedAt() == null && fact.getEndedAt() != null) {
            session.setConfidenceLevel(OperationalMonitoringConstants.CONFIDENCE_LOW);
            if (!Boolean.TRUE.equals(session.getNeedsReview())) {
                session.setNeedsReview(true);
                session.setReviewReason(OperationalMonitoringConstants.REASON_FALLBACK_ENDED_AT);
            }
            eventsToSave.add(buildEvent(null, OperationalMonitoringConstants.EVENT_SHIFT_NEEDS_REVIEW, timestamp, fact,
                    OperationalMonitoringConstants.REASON_FALLBACK_ENDED_AT, "Shift inferred using ended_at fallback"));
        }
    }

    private boolean shouldMarkStale(OperationalShiftSession session, LocalDateTime rangeEnd) {
        if (rangeEnd == null || session.getLastActivityAt() == null) {
            return false;
        }
        Duration inactivity = Duration.between(session.getLastActivityAt(), rangeEnd);
        return !inactivity.isNegative() && inactivity.compareTo(properties.getStaleCandidateThreshold()) > 0;
    }

    private boolean isCompletedStatus(OperationalTripFact fact) {
        return fact.getTripStatus() != null
                && properties.getCompletedStatuses().contains(fact.getTripStatus().toLowerCase(Locale.ROOT));
    }

    private LocalDateTime getOperationalTimestamp(OperationalTripFact fact) {
        return fact.getBookedAt() != null ? fact.getBookedAt() : fact.getEndedAt();
    }

    private String determineConfidence(OperationalTripFact fact) {
        if (fact.getBookedAt() == null) {
            return OperationalMonitoringConstants.CONFIDENCE_LOW;
        }
        if (OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_NORMALIZED_PLATE.equals(fact.getVehicleKeySource())) {
            return OperationalMonitoringConstants.CONFIDENCE_MEDIUM;
        }
        if (OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_YANGO_CAR_ID.equals(fact.getVehicleKeySource())) {
            return OperationalMonitoringConstants.CONFIDENCE_HIGH;
        }
        return OperationalMonitoringConstants.CONFIDENCE_LOW;
    }

    private String downgradeConfidenceIfNeeded(String current, OperationalTripFact fact) {
        String candidate = determineConfidence(fact);
        if (current == null) {
            return candidate;
        }
        if (OperationalMonitoringConstants.CONFIDENCE_LOW.equals(current)
                || OperationalMonitoringConstants.CONFIDENCE_LOW.equals(candidate)) {
            return OperationalMonitoringConstants.CONFIDENCE_LOW;
        }
        if (OperationalMonitoringConstants.CONFIDENCE_MEDIUM.equals(current)
                || OperationalMonitoringConstants.CONFIDENCE_MEDIUM.equals(candidate)) {
            return OperationalMonitoringConstants.CONFIDENCE_MEDIUM;
        }
        return OperationalMonitoringConstants.CONFIDENCE_HIGH;
    }

    private OperationalShiftSessionResponse toSessionResponse(OperationalShiftSession session) {
        return OperationalShiftSessionResponse.builder()
                .id(session.getId())
                .driverId(session.getDriverId())
                .vehicleKey(session.getVehicleKey())
                .vehicleKeySource(session.getVehicleKeySource())
                .vehicleId(session.getVehicleId())
                .vehiclePlateNormalized(session.getVehiclePlateNormalized())
                .openedAt(session.getOpenedAt())
                .closedAt(session.getClosedAt())
                .state(session.getState())
                .openReason(session.getOpenReason())
                .closeReason(session.getCloseReason())
                .firstTripExternalId(session.getFirstTripExternalId())
                .lastTripExternalId(session.getLastTripExternalId())
                .tripCount(session.getTripCount())
                .lastActivityAt(session.getLastActivityAt())
                .confidenceLevel(session.getConfidenceLevel())
                .needsReview(session.getNeedsReview())
                .reviewReason(session.getReviewReason())
                .build();
    }

    private OperationalShiftEventResponse toEventResponse(OperationalShiftEvent event) {
        return OperationalShiftEventResponse.builder()
                .id(event.getId())
                .operationalShiftSessionId(event.getOperationalShiftSessionId())
                .eventType(event.getEventType())
                .eventTime(event.getEventTime())
                .driverId(event.getDriverId())
                .vehicleKey(event.getVehicleKey())
                .externalTripId(event.getExternalTripId())
                .reason(event.getReason())
                .details(event.getDetails())
                .build();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeVehicleGroupKey(String vehicleKey) {
        return vehicleKey == null || vehicleKey.isBlank() ? null : vehicleKey.trim();
    }

    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}
