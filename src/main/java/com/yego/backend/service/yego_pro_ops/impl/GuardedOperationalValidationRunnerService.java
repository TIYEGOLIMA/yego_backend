package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringRunnerProperties;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.entities.OperationalTripFact;
import com.yego.backend.service.yego_pro_ops.OperationalShiftInferenceService;
import com.yego.backend.service.yego_pro_ops.OperationalShiftValidationService;
import com.yego.backend.service.yego_pro_ops.OperationalTripFactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuardedOperationalValidationRunnerService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OperationalMonitoringRunnerProperties properties;
    private final OperationalEnvironmentGuard environmentGuard;
    private final OperationalMigrationDiagnosticsService migrationDiagnosticsService;
    private final OperationalValidationSampleSelector sampleSelector;
    private final OperationalTripFactService tripFactService;
    private final OperationalShiftInferenceService shiftInferenceService;
    private final OperationalShiftValidationService validationService;

    public ExecutionReport runOnce() {
        environmentGuard.assertSafeToRun(properties);

        OperationalMigrationDiagnosticsService.MigrationDiagnostics migrationDiagnostics = migrationDiagnosticsService.inspect();
        if (!migrationDiagnostics.allOperationalTablesExist()) {
            throw new IllegalStateException("Operational migration 017 is not fully applied in the target environment");
        }

        List<String> driverIds = resolveDriverIds();
        if (driverIds.isEmpty()) {
            throw new IllegalStateException("Operational monitoring runner could not resolve any driverIds for the requested sample");
        }
        if (driverIds.size() > properties.sanitizeMaxDriverCount()) {
            throw new IllegalStateException("Operational monitoring runner resolved too many drivers for one execution");
        }

        LocalDate dateFrom = properties.getDateFrom();
        LocalDate dateTo = properties.getDateTo();
        LocalDateTime fromDateTime = dateFrom.atStartOfDay();
        LocalDateTime toDateTime = dateTo.atTime(LocalTime.MAX);

        int importedTripFacts = 0;
        int tripFactsConsidered = 0;
        int sessionsCreated = 0;
        int eventsCreated = 0;
        long needsReview = 0L;
        long autoClosed = 0L;
        long staleCandidate = 0L;
        Map<String, String> importErrors = new LinkedHashMap<>();
        Map<String, String> summaryReadinessByDriver = new LinkedHashMap<>();
        List<String> successfulDrivers = new ArrayList<>();

        for (String driverId : driverIds) {
            try {
                List<OperationalTripFact> imported = tripFactService.importFromDriverOrders(
                        driverId,
                        formatDate(dateFrom),
                        formatDate(dateTo));
                importedTripFacts += imported.size();

                OperationalShiftInferenceService.ReprocessResult reprocessResult =
                        shiftInferenceService.reprocessRange(fromDateTime, toDateTime, driverId, properties.getVehicleKey());
                tripFactsConsidered += reprocessResult.tripFactsConsidered();
                sessionsCreated += reprocessResult.sessionsCreated();
                eventsCreated += reprocessResult.eventsCreated();

                OperationalValidationSummaryResponse summary =
                        validationService.getSummary(fromDateTime, toDateTime, driverId, properties.getVehicleKey());
                if (summary != null) {
                    needsReview += nullSafeLong(summary.getNeedsReviewShiftCount());
                    autoClosed += nullSafeLong(summary.getAutoClosedByNextDriverCount());
                    staleCandidate += nullSafeLong(summary.getStaleCandidateCount());
                    summaryReadinessByDriver.put(driverId, summary.getManualReplacementReadiness());
                }
                successfulDrivers.add(driverId);
            } catch (RuntimeException ex) {
                importErrors.put(driverId, ex.getMessage());
                log.warn("Operational validation runner failed for driver {}: {}", driverId, ex.getMessage());
            }
        }

        return new ExecutionReport(
                properties.normalizedEnvironment(),
                dateFrom,
                dateTo,
                driverIds,
                successfulDrivers,
                importedTripFacts,
                tripFactsConsidered,
                sessionsCreated,
                eventsCreated,
                needsReview,
                autoClosed,
                staleCandidate,
                importErrors,
                summaryReadinessByDriver,
                migrationDiagnostics);
    }

    private List<String> resolveDriverIds() {
        List<String> explicitDriverIds = properties.normalizedDriverIds();
        if (!explicitDriverIds.isEmpty()) {
            return explicitDriverIds;
        }
        return sampleSelector.selectSample(properties).driverIds();
    }

    private String formatDate(LocalDate value) {
        return value.format(DATE_FORMATTER);
    }

    private long nullSafeLong(Number value) {
        return value == null ? 0L : value.longValue();
    }

    public record ExecutionReport(
            String environment,
            LocalDate dateFrom,
            LocalDate dateTo,
            List<String> requestedDriverIds,
            List<String> successfulDriverIds,
            int importedTripFacts,
            int tripFactsConsidered,
            int sessionsCreated,
            int eventsCreated,
            long needsReviewShiftCount,
            long autoClosedByNextDriverCount,
            long staleCandidateCount,
            Map<String, String> importErrorsByDriver,
            Map<String, String> summaryReadinessByDriver,
            OperationalMigrationDiagnosticsService.MigrationDiagnostics migrationDiagnostics) {
    }
}
