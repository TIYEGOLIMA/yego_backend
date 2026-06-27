package com.yego.backend.config.yego_pro_ops;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OperationalMonitoringProperties {

    private static final int DEFAULT_READ_LIMIT = 200;
    private static final int MAX_READ_LIMIT = 1_000;

    private final ZoneId zoneId;
    private final Duration staleCandidateThreshold;
    private final Duration startDeltaTolerance;
    private final Duration endDeltaTolerance;
    private final Set<String> completedStatuses;
    private final boolean mirrorModeEnabled;

    public OperationalMonitoringProperties(
            @Value("${operational.monitoring.timezone:America/Lima}") String timezone,
            @Value("${operational.monitoring.stale-threshold-hours:8}") long staleThresholdHours,
            @Value("${operational.monitoring.validation.start-delta-tolerance-minutes:90}") long startDeltaToleranceMinutes,
            @Value("${operational.monitoring.validation.end-delta-tolerance-minutes:90}") long endDeltaToleranceMinutes,
            @Value("${operational.monitoring.completed-statuses:complete}") String completedStatusesRaw,
            @Value("${operational.monitoring.mirror-mode:true}") boolean mirrorModeEnabled) {
        this.zoneId = ZoneId.of(timezone);
        this.staleCandidateThreshold = Duration.ofHours(staleThresholdHours);
        this.startDeltaTolerance = Duration.ofMinutes(startDeltaToleranceMinutes);
        this.endDeltaTolerance = Duration.ofMinutes(endDeltaToleranceMinutes);
        this.completedStatuses = Arrays.stream(completedStatusesRaw.split(","))
                .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.mirrorModeEnabled = mirrorModeEnabled;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public Duration getStaleCandidateThreshold() {
        return staleCandidateThreshold;
    }

    public Set<String> getCompletedStatuses() {
        return completedStatuses;
    }

    public Duration getStartDeltaTolerance() {
        return startDeltaTolerance;
    }

    public Duration getEndDeltaTolerance() {
        return endDeltaTolerance;
    }

    public boolean isMirrorModeEnabled() {
        return mirrorModeEnabled;
    }

    public int sanitizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_READ_LIMIT;
        }
        return Math.min(requestedLimit, MAX_READ_LIMIT);
    }

    public int sanitizeOffset(Integer requestedOffset) {
        if (requestedOffset == null || requestedOffset < 0) {
            return 0;
        }
        return requestedOffset;
    }
}
