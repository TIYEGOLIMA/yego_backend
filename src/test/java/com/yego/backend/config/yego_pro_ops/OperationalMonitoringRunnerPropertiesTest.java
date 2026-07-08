package com.yego.backend.config.yego_pro_ops;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalMonitoringRunnerPropertiesTest {

    private final OperationalMonitoringRunnerProperties properties = new OperationalMonitoringRunnerProperties();

    @Test
    void defaultsAreClosedByDefault() {
        assertFalse(properties.isEnabled());
        assertFalse(properties.isConfirmWritesToOperationalTables());
        assertEquals("unknown", properties.normalizedEnvironment());
        assertEquals(1, properties.getMaxRangeDays());
        assertEquals(5, properties.sanitizeDefaultDriverCount());
        assertEquals(20, properties.sanitizeMaxDriverCount());
        assertTrue(properties.isUseManualSampleSelector());
    }

    @Test
    void defaultDriverCountIsCappedToConfiguredMaximum() {
        properties.setDefaultDriverCount(30);
        properties.setMaxDriverCount(7);

        assertEquals(7, properties.sanitizeDefaultDriverCount());
    }

    @Test
    void maxDriverCountNeverExceedsHardCap() {
        properties.setMaxDriverCount(99);

        assertEquals(20, properties.sanitizeMaxDriverCount());
    }
}
