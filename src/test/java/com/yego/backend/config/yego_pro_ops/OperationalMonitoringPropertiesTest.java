package com.yego.backend.config.yego_pro_ops;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalMonitoringPropertiesTest {

    private final OperationalMonitoringProperties properties = new OperationalMonitoringProperties(
            "America/Lima",
            8,
            "complete",
            true);

    @Test
    void sanitizeLimitUsesDefaultWhenNullOrNonPositive() {
        assertEquals(200, properties.sanitizeLimit(null));
        assertEquals(200, properties.sanitizeLimit(0));
        assertEquals(200, properties.sanitizeLimit(-1));
    }

    @Test
    void sanitizeLimitCapsAtMaximum() {
        assertEquals(1_000, properties.sanitizeLimit(1_500));
    }

    @Test
    void sanitizeOffsetUsesZeroWhenNullOrNegative() {
        assertEquals(0, properties.sanitizeOffset(null));
        assertEquals(0, properties.sanitizeOffset(-1));
    }
}
