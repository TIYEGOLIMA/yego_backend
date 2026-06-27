package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.config.yego_pro_ops.OperationalMonitoringRunnerProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationalEnvironmentGuardTest {

    private final OperationalEnvironmentGuard guard = new OperationalEnvironmentGuard();

    @Test
    void failsWhenRunnerIsDisabledByDefault() {
        OperationalMonitoringRunnerProperties properties = validProperties();
        properties.setEnabled(false);

        assertThrows(IllegalStateException.class, () -> guard.assertSafeToRun(properties));
    }

    @Test
    void failsWhenEnvironmentIsProdOrUnknown() {
        OperationalMonitoringRunnerProperties prod = validProperties();
        prod.setEnvironment("prod");

        OperationalMonitoringRunnerProperties unknown = validProperties();
        unknown.setEnvironment("unknown");

        assertThrows(IllegalStateException.class, () -> guard.assertSafeToRun(prod));
        assertThrows(IllegalStateException.class, () -> guard.assertSafeToRun(unknown));
    }

    @Test
    void failsWhenConfirmWritesIsDisabled() {
        OperationalMonitoringRunnerProperties properties = validProperties();
        properties.setConfirmWritesToOperationalTables(false);

        assertThrows(IllegalStateException.class, () -> guard.assertSafeToRun(properties));
    }

    @Test
    void failsWhenDateRangeExceedsMaximum() {
        OperationalMonitoringRunnerProperties properties = validProperties();
        properties.setDateTo(properties.getDateFrom().plusDays(1));

        assertThrows(IllegalStateException.class, () -> guard.assertSafeToRun(properties));
    }

    @Test
    void failsWhenDriverCountExceedsMaximum() {
        OperationalMonitoringRunnerProperties properties = validProperties();
        properties.setDriverIds(List.of("1", "2", "3", "4", "5", "6"));
        properties.setMaxDriverCount(5);

        assertThrows(IllegalStateException.class, () -> guard.assertSafeToRun(properties));
    }

    @Test
    void allowsSafeLocalExecution() {
        assertDoesNotThrow(() -> guard.assertSafeToRun(validProperties()));
    }

    private OperationalMonitoringRunnerProperties validProperties() {
        OperationalMonitoringRunnerProperties properties = new OperationalMonitoringRunnerProperties();
        properties.setEnabled(true);
        properties.setEnvironment("local");
        properties.setConfirmWritesToOperationalTables(true);
        properties.setDateFrom(LocalDate.of(2026, 6, 23));
        properties.setDateTo(LocalDate.of(2026, 6, 23));
        properties.setDriverIds(List.of("driver-1", "driver-2"));
        return properties;
    }
}
