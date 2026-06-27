package com.yego.backend.config.yego_pro_ops;

import com.yego.backend.service.yego_pro_ops.impl.GuardedOperationalValidationRunnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "operational.monitoring.runner", name = "enabled", havingValue = "true")
@Slf4j
public class GuardedOperationalValidationApplicationRunner implements ApplicationRunner {

    private final GuardedOperationalValidationRunnerService runnerService;

    @Override
    public void run(ApplicationArguments args) {
        GuardedOperationalValidationRunnerService.ExecutionReport report = runnerService.runOnce();
        log.info("Operational validation runner environment={}", report.environment());
        log.info("Operational validation runner range={}..{}", report.dateFrom(), report.dateTo());
        log.info("Operational validation runner driversRequested={} driversSucceeded={}",
                report.requestedDriverIds(), report.successfulDriverIds());
        log.info("Operational validation runner importedTripFacts={} tripFactsConsidered={} sessionsCreated={} eventsCreated={}",
                report.importedTripFacts(), report.tripFactsConsidered(), report.sessionsCreated(), report.eventsCreated());
        log.info("Operational validation runner needsReview={} autoClosed={} staleCandidate={}",
                report.needsReviewShiftCount(), report.autoClosedByNextDriverCount(), report.staleCandidateCount());
        log.info("Operational validation runner summaryReadinessByDriver={}", report.summaryReadinessByDriver());
        if (!report.importErrorsByDriver().isEmpty()) {
            log.warn("Operational validation runner importErrorsByDriver={}", report.importErrorsByDriver());
        }
        log.info("Operational validation migration diagnostics mechanism={} ddlAuto={} historyTable={} allOperationalTablesExist={}",
                report.migrationDiagnostics().migrationMechanism(),
                report.migrationDiagnostics().ddlAutoMode(),
                report.migrationDiagnostics().migrationHistoryTableName(),
                report.migrationDiagnostics().allOperationalTablesExist());
    }
}
