package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringRunnerProperties;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Component
public class OperationalEnvironmentGuard {

    private static final Set<String> ALLOWED_ENVIRONMENTS = Set.of("local", "dev", "staging");

    public void assertSafeToRun(OperationalMonitoringRunnerProperties properties) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Operational monitoring runner is disabled");
        }
        if (!ALLOWED_ENVIRONMENTS.contains(properties.normalizedEnvironment())) {
            throw new IllegalStateException("Operational monitoring runner only supports local/dev/staging environments");
        }
        if (!properties.isConfirmWritesToOperationalTables()) {
            throw new IllegalStateException("Operational monitoring runner requires confirmWritesToOperationalTables=true");
        }
        if (properties.getDateFrom() == null || properties.getDateTo() == null) {
            throw new IllegalStateException("Operational monitoring runner requires dateFrom and dateTo");
        }

        long rangeDays = ChronoUnit.DAYS.between(properties.getDateFrom(), properties.getDateTo()) + 1L;
        if (rangeDays <= 0) {
            throw new IllegalStateException("Operational monitoring runner requires dateTo >= dateFrom");
        }
        if (rangeDays > properties.getMaxRangeDays()) {
            throw new IllegalStateException("Operational monitoring runner exceeds the max safe date range");
        }

        List<String> driverIds = properties.normalizedDriverIds();
        if (!driverIds.isEmpty() && driverIds.size() > properties.sanitizeMaxDriverCount()) {
            throw new IllegalStateException("Operational monitoring runner exceeds the max safe driver count");
        }
        if (driverIds.isEmpty() && !properties.isUseManualSampleSelector()) {
            throw new IllegalStateException("Operational monitoring runner requires driverIds or manual sample selection");
        }
    }
}
