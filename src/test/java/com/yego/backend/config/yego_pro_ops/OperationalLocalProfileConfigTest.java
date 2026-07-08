package com.yego.backend.config.yego_pro_ops;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalLocalProfileConfigTest {

    @Test
    void operationalLocalProfileUsesOnlyLocalSafeDefaults() throws IOException {
        String profile = loadResource("application-operational-local.yml");

        assertTrue(profile.contains("${OPERATIONAL_LOCAL_DB_HOST:localhost}"));
        assertTrue(profile.contains("${OPERATIONAL_LOCAL_DB_PORT:54329}"));
        assertTrue(profile.contains("${OPERATIONAL_LOCAL_DB_NAME:yego_operational_local}"));
        assertTrue(profile.contains("${OPERATIONAL_LOCAL_DB_USER:yego_local}"));
        assertTrue(profile.contains("${OPERATIONAL_LOCAL_DB_PASSWORD:yego_local}"));
        assertTrue(profile.contains("enabled: false"));
        assertTrue(profile.contains("environment: local"));
        assertTrue(profile.contains("confirm-writes-to-operational-tables: false"));

        assertFalse(profile.contains("168.119.226.236"));
        assertFalse(profile.contains("yego_integral"));
    }

    private String loadResource(String resourceName) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName);
        assertNotNull(stream, "Resource must exist: " + resourceName);
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
