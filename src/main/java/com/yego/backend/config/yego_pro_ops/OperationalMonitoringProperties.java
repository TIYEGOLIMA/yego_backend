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

    private final ZoneId zoneId;
    private final Duration staleCandidateThreshold;
    private final Set<String> completedStatuses;
    private final boolean mirrorModeEnabled;

    public OperationalMonitoringProperties(
            @Value("${operational.monitoring.timezone:America/Lima}") String timezone,
            @Value("${operational.monitoring.stale-threshold-hours:8}") long staleThresholdHours,
            @Value("${operational.monitoring.completed-statuses:complete}") String completedStatusesRaw,
            @Value("${operational.monitoring.mirror-mode:true}") boolean mirrorModeEnabled) {
        this.zoneId = ZoneId.of(timezone);
        this.staleCandidateThreshold = Duration.ofHours(staleThresholdHours);
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

    public boolean isMirrorModeEnabled() {
        return mirrorModeEnabled;
    }
}
