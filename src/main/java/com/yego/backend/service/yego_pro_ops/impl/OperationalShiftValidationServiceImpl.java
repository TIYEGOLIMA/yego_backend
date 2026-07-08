package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringProperties;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalManualComparisonResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationCoverageResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationMismatchResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalShiftSession;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalTripFact;
import com.yego.backend.repository.yego_pro_ops.OperationalShiftSessionRepository;
import com.yego.backend.repository.yego_pro_ops.OperationalTripFactRepository;
import com.yego.backend.service.yego_pro_ops.OperationalManualShiftReadAdapter;
import com.yego.backend.service.yego_pro_ops.OperationalShiftValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperationalShiftValidationServiceImpl implements OperationalShiftValidationService {

    private static final String STATUS_MATCHED = "MATCHED";
    private static final String STATUS_AUTO_ONLY = "AUTO_ONLY";
    private static final String STATUS_MANUAL_ONLY = "MANUAL_ONLY";
    private static final String STATUS_TIME_DELTA_HIGH = "TIME_DELTA_HIGH";
    private static final String STATUS_VEHICLE_MISMATCH = "VEHICLE_MISMATCH";
    private static final String STATUS_NEEDS_REVIEW = "NEEDS_REVIEW";
    private static final String STATUS_INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

    private static final String VEHICLE_MATCH = "MATCH";
    private static final String VEHICLE_MISMATCH = "MISMATCH";
    private static final String VEHICLE_NOT_AVAILABLE = "NOT_AVAILABLE";

    private final OperationalShiftSessionRepository operationalShiftSessionRepository;
    private final OperationalTripFactRepository operationalTripFactRepository;
    private final OperationalManualShiftReadAdapter manualShiftReadAdapter;
    private final OperationalMonitoringProperties properties;

    @Override
    @Transactional(readOnly = true)
    public OperationalValidationSummaryResponse getSummary(LocalDateTime from, LocalDateTime to, String driverId, String vehicleKey) {
        ValidationDataset dataset = loadDataset(from, to, driverId, vehicleKey);
        ValidationComputation computation = computeValidation(dataset);

        List<Long> startDeltas = computation.comparisons().stream()
                .map(OperationalManualComparisonResponse::getStartDeltaMinutes)
                .filter(Objects::nonNull)
                .toList();
        List<Long> endDeltas = computation.comparisons().stream()
                .map(OperationalManualComparisonResponse::getEndDeltaMinutes)
                .filter(Objects::nonNull)
                .toList();

        double matchedShiftPct = percentage(computation.matchedShiftCount(), Math.max(dataset.operationalShifts().size(), dataset.manualShifts().size()));
        String readiness = determineReadiness(
                dataset.vehicleKeyCoveragePct(),
                dataset.needsReviewShiftPct(),
                matchedShiftPct,
                percentile(startDeltas, 95),
                percentile(endDeltas, 95),
                dataset.operationalShifts().isEmpty());

        return OperationalValidationSummaryResponse.builder()
                .operationalShiftCount(dataset.operationalShifts().size())
                .manualShiftCount(dataset.manualShifts().size())
                .matchedShiftCount(computation.matchedShiftCount())
                .unmatchedOperationalShiftCount(computation.autoOnlyCount())
                .unmatchedManualShiftCount(computation.manualOnlyCount())
                .operationalTripFactCount(dataset.tripFacts().size())
                .tripFactsWithVehicleKeyCount(dataset.tripFactsWithVehicleKeyCount())
                .tripFactsMissingVehicleKeyCount(dataset.tripFactsMissingVehicleKeyCount())
                .vehicleKeyCoveragePct(dataset.vehicleKeyCoveragePct())
                .needsReviewShiftCount(dataset.needsReviewShiftCount())
                .needsReviewShiftPct(dataset.needsReviewShiftPct())
                .autoClosedByNextDriverCount(dataset.autoClosedByNextDriverCount())
                .staleCandidateCount(dataset.staleCandidateCount())
                .averageStartDeltaMinutes(average(startDeltas))
                .averageEndDeltaMinutes(average(endDeltas))
                .p50StartDeltaMinutes(percentile(startDeltas, 50))
                .p95StartDeltaMinutes(percentile(startDeltas, 95))
                .p50EndDeltaMinutes(percentile(endDeltas, 50))
                .p95EndDeltaMinutes(percentile(endDeltas, 95))
                .manualReplacementReadiness(readiness)
                .readinessReason(buildReadinessReason(readiness, dataset, matchedShiftPct, startDeltas, endDeltas))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationalManualComparisonResponse> getManualComparison(
            LocalDateTime from,
            LocalDateTime to,
            String driverId,
            String vehicleKey,
            Integer limit,
            Integer offset) {
        ValidationComputation computation = computeValidation(loadDataset(from, to, driverId, vehicleKey));
        return paginate(computation.comparisons(), limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationalValidationMismatchResponse> getMismatches(
            LocalDateTime from,
            LocalDateTime to,
            String driverId,
            String type,
            Integer limit,
            Integer offset) {
        List<OperationalValidationMismatchResponse> mismatches = computeValidation(loadDataset(from, to, driverId, null))
                .comparisons().stream()
                .filter(comparison -> !STATUS_MATCHED.equals(comparison.getComparisonStatus()))
                .filter(comparison -> type == null || type.isBlank() || comparison.getComparisonStatus().equalsIgnoreCase(type.trim()))
                .map(comparison -> OperationalValidationMismatchResponse.builder()
                        .operationalShiftId(comparison.getOperationalShiftId())
                        .manualShiftId(comparison.getManualShiftId())
                        .driverId(comparison.getDriverId())
                        .mismatchType(comparison.getComparisonStatus())
                        .mismatchReason(comparison.getMismatchReason())
                        .operationalOpenedAt(comparison.getOperationalOpenedAt())
                        .operationalClosedAt(comparison.getOperationalClosedAt())
                        .operationalState(comparison.getOperationalState())
                        .operationalVehicleKey(comparison.getOperationalVehicleKey())
                        .operationalPlateNormalized(comparison.getOperationalPlateNormalized())
                        .manualStartedAt(comparison.getManualStartedAt())
                        .manualClosedAt(comparison.getManualClosedAt())
                        .manualStatus(comparison.getManualStatus())
                        .manualPlateNormalized(comparison.getManualPlateNormalized())
                        .vehicleComparison(comparison.getVehicleComparison())
                        .startDeltaMinutes(comparison.getStartDeltaMinutes())
                        .endDeltaMinutes(comparison.getEndDeltaMinutes())
                        .build())
                .toList();
        return paginate(mismatches, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public OperationalValidationCoverageResponse getCoverage(LocalDateTime from, LocalDateTime to, String driverId, String vehicleKey) {
        ValidationDataset dataset = loadDataset(from, to, driverId, vehicleKey);
        long highConfidence = dataset.operationalShifts().stream()
                .filter(shift -> OperationalMonitoringConstants.CONFIDENCE_HIGH.equalsIgnoreCase(shift.getConfidenceLevel()))
                .count();
        long mediumConfidence = dataset.operationalShifts().stream()
                .filter(shift -> OperationalMonitoringConstants.CONFIDENCE_MEDIUM.equalsIgnoreCase(shift.getConfidenceLevel()))
                .count();
        long lowConfidence = dataset.operationalShifts().stream()
                .filter(shift -> OperationalMonitoringConstants.CONFIDENCE_LOW.equalsIgnoreCase(shift.getConfidenceLevel()) || shift.getConfidenceLevel() == null)
                .count();

        return OperationalValidationCoverageResponse.builder()
                .operationalTripFactCount(dataset.tripFacts().size())
                .tripFactsWithDriverIdCount(dataset.tripFacts().stream().filter(fact -> fact.getDriverId() != null && !fact.getDriverId().isBlank()).count())
                .tripFactsWithVehicleKeyCount(dataset.tripFactsWithVehicleKeyCount())
                .tripFactsWithNormalizedPlateCount(dataset.tripFacts().stream().filter(fact -> fact.getVehiclePlateNormalized() != null && !fact.getVehiclePlateNormalized().isBlank()).count())
                .tripFactsMissingVehicleCount(dataset.tripFactsMissingVehicleKeyCount())
                .operationalShiftCount(dataset.operationalShifts().size())
                .highConfidenceShiftCount(highConfidence)
                .mediumConfidenceShiftCount(mediumConfidence)
                .lowConfidenceShiftCount(lowConfidence)
                .needsReviewShiftCount(dataset.needsReviewShiftCount())
                .vehicleKeyCoveragePct(dataset.vehicleKeyCoveragePct())
                .needsReviewShiftPct(dataset.needsReviewShiftPct())
                .build();
    }

    private ValidationDataset loadDataset(LocalDateTime from, LocalDateTime to, String driverId, String vehicleKey) {
        String normalizedDriverId = normalize(driverId);
        String normalizedVehicleKey = normalize(vehicleKey);
        List<OperationalTripFact> tripFacts = operationalTripFactRepository.findForValidation(from, to, normalizedDriverId, normalizedVehicleKey);
        List<OperationalShiftSession> operationalShifts = operationalShiftSessionRepository.findForValidation(from, to, normalizedDriverId, normalizedVehicleKey);
        List<OperationalManualShiftReadAdapter.ManualShiftSnapshot> manualShifts = manualShiftReadAdapter.findManualShifts(from, to, normalizedDriverId, normalizedVehicleKey);

        long tripFactsWithVehicleKeyCount = tripFacts.stream().filter(fact -> fact.getVehicleKey() != null && !fact.getVehicleKey().isBlank()).count();
        long needsReviewShiftCount = operationalShifts.stream().filter(shift -> Boolean.TRUE.equals(shift.getNeedsReview())).count();
        return new ValidationDataset(
                tripFacts,
                operationalShifts,
                manualShifts,
                tripFactsWithVehicleKeyCount,
                tripFacts.size() - tripFactsWithVehicleKeyCount,
                needsReviewShiftCount,
                operationalShifts.stream().filter(shift -> OperationalMonitoringConstants.STATE_AUTO_CLOSED_BY_NEXT_DRIVER.equalsIgnoreCase(shift.getState())).count(),
                operationalShifts.stream().filter(shift -> OperationalMonitoringConstants.STATE_STALE_CANDIDATE.equalsIgnoreCase(shift.getState())).count());
    }

    private ValidationComputation computeValidation(ValidationDataset dataset) {
        Map<UUID, OperationalManualShiftReadAdapter.ManualShiftSnapshot> matchedManuals = new HashMap<>();
        List<OperationalManualComparisonResponse> comparisons = new ArrayList<>();
        List<OperationalManualShiftReadAdapter.ManualShiftSnapshot> manuals = dataset.manualShifts().stream()
                .sorted(Comparator.comparing(OperationalManualShiftReadAdapter.ManualShiftSnapshot::startedAt).thenComparing(OperationalManualShiftReadAdapter.ManualShiftSnapshot::shiftSessionId))
                .toList();

        for (OperationalShiftSession operationalShift : dataset.operationalShifts().stream()
                .sorted(Comparator.comparing(OperationalShiftSession::getOpenedAt).thenComparing(OperationalShiftSession::getId))
                .toList()) {
            MatchCandidate bestCandidate = null;
            for (OperationalManualShiftReadAdapter.ManualShiftSnapshot manualShift : manuals) {
                if (matchedManuals.containsKey(manualShift.shiftSessionId())) {
                    continue;
                }
                if (!safeEquals(operationalShift.getDriverId(), manualShift.driverId())) {
                    continue;
                }
                MatchCandidate candidate = scoreCandidate(operationalShift, manualShift);
                if (candidate == null) {
                    continue;
                }
                if (bestCandidate == null || candidate.compareTo(bestCandidate) < 0) {
                    bestCandidate = candidate;
                }
            }

            if (bestCandidate == null) {
                comparisons.add(toAutoOnlyComparison(operationalShift));
                continue;
            }

            matchedManuals.put(bestCandidate.manual().shiftSessionId(), bestCandidate.manual());
            comparisons.add(toPairedComparison(operationalShift, bestCandidate));
        }

        for (OperationalManualShiftReadAdapter.ManualShiftSnapshot manualShift : manuals) {
            if (matchedManuals.containsKey(manualShift.shiftSessionId())) {
                continue;
            }
            comparisons.add(toManualOnlyComparison(manualShift));
        }

        long matchedShiftCount = comparisons.stream()
                .filter(response -> STATUS_MATCHED.equals(response.getComparisonStatus()) || STATUS_INSUFFICIENT_DATA.equals(response.getComparisonStatus()))
                .count();
        long autoOnlyCount = comparisons.stream().filter(response -> STATUS_AUTO_ONLY.equals(response.getComparisonStatus())).count();
        long manualOnlyCount = comparisons.stream().filter(response -> STATUS_MANUAL_ONLY.equals(response.getComparisonStatus())).count();

        return new ValidationComputation(
                comparisons.stream()
                        .sorted(Comparator.comparing(OperationalManualComparisonResponse::getOperationalOpenedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(OperationalManualComparisonResponse::getManualStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList(),
                matchedShiftCount,
                autoOnlyCount,
                manualOnlyCount);
    }

    private MatchCandidate scoreCandidate(OperationalShiftSession operationalShift, OperationalManualShiftReadAdapter.ManualShiftSnapshot manualShift) {
        long startDelta = absoluteMinutes(operationalShift.getOpenedAt(), manualShift.startedAt());
        long endDelta = absoluteMinutes(operationalShift.getClosedAt(), manualShift.closedAt());
        long overlapMinutes = overlapMinutes(operationalShift.getOpenedAt(), operationalShift.getClosedAt(), manualShift.startedAt(), manualShift.closedAt());
        boolean withinStartTolerance = startDelta <= properties.getStartDeltaTolerance().toMinutes();
        boolean withinEndTolerance = endDelta <= properties.getEndDeltaTolerance().toMinutes();
        boolean likelyMatch = withinStartTolerance || overlapMinutes > 0;
        if (!likelyMatch && startDelta > properties.getStartDeltaTolerance().toMinutes() * 4L) {
            return null;
        }
        return new MatchCandidate(manualShift, startDelta, endDelta, overlapMinutes, withinStartTolerance, withinEndTolerance);
    }

    private OperationalManualComparisonResponse toPairedComparison(OperationalShiftSession operationalShift, MatchCandidate candidate) {
        String vehicleComparison = compareVehicle(operationalShift, candidate.manual());
        String comparisonStatus = STATUS_MATCHED;
        String mismatchReason = "Matched by driver and temporal proximity";

        if (Boolean.TRUE.equals(operationalShift.getNeedsReview())) {
            comparisonStatus = STATUS_NEEDS_REVIEW;
            mismatchReason = operationalShift.getReviewReason() == null ? "Operational shift requires review" : operationalShift.getReviewReason();
        } else if (VEHICLE_MISMATCH.equals(vehicleComparison)) {
            comparisonStatus = STATUS_VEHICLE_MISMATCH;
            mismatchReason = "Manual plate does not match operational plate";
        } else if (!candidate.withinStartTolerance() || (!candidate.endDeltaUnavailable() && !candidate.withinEndTolerance())) {
            comparisonStatus = STATUS_TIME_DELTA_HIGH;
            mismatchReason = "Start or end delta exceeds tolerance";
        } else if (operationalShift.getClosedAt() == null || candidate.manual().closedAt() == null) {
            comparisonStatus = STATUS_INSUFFICIENT_DATA;
            mismatchReason = "One side has no closing timestamp";
        }

        return OperationalManualComparisonResponse.builder()
                .operationalShiftId(operationalShift.getId())
                .manualShiftId(candidate.manual().shiftSessionId())
                .driverId(operationalShift.getDriverId())
                .comparisonStatus(comparisonStatus)
                .mismatchReason(mismatchReason)
                .operationalOpenedAt(operationalShift.getOpenedAt())
                .operationalClosedAt(operationalShift.getClosedAt())
                .operationalState(operationalShift.getState())
                .operationalVehicleKey(operationalShift.getVehicleKey())
                .operationalPlateNormalized(operationalShift.getVehiclePlateNormalized())
                .manualStartedAt(candidate.manual().startedAt())
                .manualClosedAt(candidate.manual().closedAt())
                .manualStatus(candidate.manual().status())
                .manualPlateNormalized(candidate.manual().plateNormalized())
                .vehicleComparison(vehicleComparison)
                .startDeltaMinutes(candidate.startDeltaMinutes())
                .endDeltaMinutes(candidate.endDeltaUnavailable() ? null : candidate.endDeltaMinutes())
                .build();
    }

    private OperationalManualComparisonResponse toAutoOnlyComparison(OperationalShiftSession operationalShift) {
        return OperationalManualComparisonResponse.builder()
                .operationalShiftId(operationalShift.getId())
                .driverId(operationalShift.getDriverId())
                .comparisonStatus(Boolean.TRUE.equals(operationalShift.getNeedsReview()) ? STATUS_NEEDS_REVIEW : STATUS_AUTO_ONLY)
                .mismatchReason(Boolean.TRUE.equals(operationalShift.getNeedsReview())
                        ? "Operational shift requires review and has no reliable manual match"
                        : "No manual shift found for same driver in tolerance window")
                .operationalOpenedAt(operationalShift.getOpenedAt())
                .operationalClosedAt(operationalShift.getClosedAt())
                .operationalState(operationalShift.getState())
                .operationalVehicleKey(operationalShift.getVehicleKey())
                .operationalPlateNormalized(operationalShift.getVehiclePlateNormalized())
                .vehicleComparison(VEHICLE_NOT_AVAILABLE)
                .build();
    }

    private OperationalManualComparisonResponse toManualOnlyComparison(OperationalManualShiftReadAdapter.ManualShiftSnapshot manualShift) {
        return OperationalManualComparisonResponse.builder()
                .manualShiftId(manualShift.shiftSessionId())
                .driverId(manualShift.driverId())
                .comparisonStatus(STATUS_MANUAL_ONLY)
                .mismatchReason("No operational shift found for same driver in tolerance window")
                .manualStartedAt(manualShift.startedAt())
                .manualClosedAt(manualShift.closedAt())
                .manualStatus(manualShift.status())
                .manualPlateNormalized(manualShift.plateNormalized())
                .vehicleComparison(VEHICLE_NOT_AVAILABLE)
                .build();
    }

    private String compareVehicle(OperationalShiftSession operationalShift, OperationalManualShiftReadAdapter.ManualShiftSnapshot manualShift) {
        if (manualShift.plateNormalized() == null || manualShift.plateNormalized().isBlank()) {
            return VEHICLE_NOT_AVAILABLE;
        }
        if (operationalShift.getVehiclePlateNormalized() == null || operationalShift.getVehiclePlateNormalized().isBlank()) {
            return VEHICLE_NOT_AVAILABLE;
        }
        return manualShift.plateNormalized().equals(operationalShift.getVehiclePlateNormalized())
                ? VEHICLE_MATCH
                : VEHICLE_MISMATCH;
    }

    private <T> List<T> paginate(List<T> input, Integer limit, Integer offset) {
        int safeLimit = properties.sanitizeLimit(limit);
        int safeOffset = properties.sanitizeOffset(offset);
        if (safeOffset >= input.size()) {
            return List.of();
        }
        int endIndex = Math.min(input.size(), safeOffset + safeLimit);
        return input.subList(safeOffset, endIndex);
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0d;
        }
        return (numerator * 100.0d) / denominator;
    }

    private Double average(List<Long> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.stream().mapToLong(Long::longValue).average().orElse(0.0d);
    }

    private Double percentile(List<Long> values, int percentile) {
        if (values.isEmpty()) {
            return null;
        }
        List<Long> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil((percentile / 100.0d) * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index).doubleValue();
    }

    private String determineReadiness(
            double vehicleKeyCoveragePct,
            double needsReviewShiftPct,
            double matchedShiftPct,
            Double p95StartDeltaMinutes,
            Double p95EndDeltaMinutes,
            boolean insufficientOperationalData) {
        if (insufficientOperationalData) {
            return "NOT_READY";
        }
        if (vehicleKeyCoveragePct >= 95
                && matchedShiftPct >= 90
                && needsReviewShiftPct <= 5
                && (p95StartDeltaMinutes == null || p95StartDeltaMinutes <= 60)
                && (p95EndDeltaMinutes == null || p95EndDeltaMinutes <= 60)) {
            return "READY_CANDIDATE";
        }
        if (vehicleKeyCoveragePct >= 85 && matchedShiftPct >= 75 && needsReviewShiftPct <= 10) {
            return "PROMISING";
        }
        if (vehicleKeyCoveragePct >= 70 && matchedShiftPct >= 60 && needsReviewShiftPct <= 20) {
            return "WATCH";
        }
        return "NOT_READY";
    }

    private String buildReadinessReason(
            String readiness,
            ValidationDataset dataset,
            double matchedShiftPct,
            List<Long> startDeltas,
            List<Long> endDeltas) {
        if (dataset.operationalShifts().isEmpty()) {
            return "INSUFFICIENT_OPERATIONAL_DATA";
        }
        return String.format(Locale.ROOT,
                "coverage=%.1f%% matched=%.1f%% needsReview=%.1f%% p95Start=%s p95End=%s readiness=%s",
                dataset.vehicleKeyCoveragePct(),
                matchedShiftPct,
                dataset.needsReviewShiftPct(),
                formatMetric(percentile(startDeltas, 95)),
                formatMetric(percentile(endDeltas, 95)),
                readiness);
    }

    private String formatMetric(Double value) {
        return value == null ? "n/a" : String.format(Locale.ROOT, "%.1f", value);
    }

    private long overlapMinutes(LocalDateTime leftStart, LocalDateTime leftEnd, LocalDateTime rightStart, LocalDateTime rightEnd) {
        if (leftStart == null || rightStart == null) {
            return 0L;
        }
        LocalDateTime normalizedLeftEnd = leftEnd != null ? leftEnd : leftStart;
        LocalDateTime normalizedRightEnd = rightEnd != null ? rightEnd : rightStart;
        LocalDateTime overlapStart = leftStart.isAfter(rightStart) ? leftStart : rightStart;
        LocalDateTime overlapEnd = normalizedLeftEnd.isBefore(normalizedRightEnd) ? normalizedLeftEnd : normalizedRightEnd;
        if (overlapEnd.isBefore(overlapStart)) {
            return 0L;
        }
        return Duration.between(overlapStart, overlapEnd).toMinutes();
    }

    private long absoluteMinutes(LocalDateTime left, LocalDateTime right) {
        if (left == null || right == null) {
            return Long.MAX_VALUE;
        }
        return Math.abs(Duration.between(left, right).toMinutes());
    }

    private boolean safeEquals(String left, String right) {
        return Objects.equals(left, right);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    record ValidationDataset(
            List<OperationalTripFact> tripFacts,
            List<OperationalShiftSession> operationalShifts,
            List<OperationalManualShiftReadAdapter.ManualShiftSnapshot> manualShifts,
            long tripFactsWithVehicleKeyCount,
            long tripFactsMissingVehicleKeyCount,
            long needsReviewShiftCount,
            long autoClosedByNextDriverCount,
            long staleCandidateCount) {

        double vehicleKeyCoveragePct() {
            if (tripFacts.isEmpty()) {
                return 0.0d;
            }
            return (tripFactsWithVehicleKeyCount * 100.0d) / tripFacts.size();
        }

        double needsReviewShiftPct() {
            if (operationalShifts.isEmpty()) {
                return 0.0d;
            }
            return (needsReviewShiftCount * 100.0d) / operationalShifts.size();
        }
    }

    record MatchCandidate(
            OperationalManualShiftReadAdapter.ManualShiftSnapshot manual,
            long startDeltaMinutes,
            long endDeltaMinutes,
            long overlapMinutes,
            boolean withinStartTolerance,
            boolean withinEndTolerance) implements Comparable<MatchCandidate> {

        boolean endDeltaUnavailable() {
            return endDeltaMinutes == Long.MAX_VALUE;
        }

        @Override
        public int compareTo(MatchCandidate other) {
            int toleranceCompare = Boolean.compare(other.withinStartTolerance, withinStartTolerance);
            if (toleranceCompare != 0) {
                return toleranceCompare;
            }
            int overlapCompare = Long.compare(other.overlapMinutes, overlapMinutes);
            if (overlapCompare != 0) {
                return overlapCompare;
            }
            return Long.compare(startDeltaMinutes, other.startDeltaMinutes);
        }
    }

    record ValidationComputation(
            List<OperationalManualComparisonResponse> comparisons,
            long matchedShiftCount,
            long autoOnlyCount,
            long manualOnlyCount) {
    }
}
