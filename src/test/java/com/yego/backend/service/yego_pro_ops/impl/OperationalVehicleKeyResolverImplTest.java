package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.request.OperationalTripFactInput;
import com.yego.backend.service.yego_pro_ops.OperationalVehicleKeyResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalVehicleKeyResolverImplTest {

    private final OperationalVehicleKeyResolver resolver = new OperationalVehicleKeyResolverImpl();

    @Test
    void normalizePlateRemovesSeparatorsAndUppercases() {
        assertEquals("ABC123", resolver.normalizePlate(" ab-c 123 "));
    }

    @Test
    void resolveUsesCarIdWhenAvailable() {
        OperationalVehicleKeyResolver.Resolution resolution = resolver.resolve(OperationalTripFactInput.builder()
                .externalTripId("trip-1")
                .driverId("driver-1")
                .vehicleId("car-123")
                .vehiclePlate("abc-123")
                .build());

        assertEquals("car-123", resolution.vehicleKey());
        assertEquals(OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_YANGO_CAR_ID, resolution.vehicleKeySource());
        assertEquals("ABC123", resolution.normalizedPlate());
    }

    @Test
    void resolveFallsBackToNormalizedPlate() {
        OperationalVehicleKeyResolver.Resolution resolution = resolver.resolve(OperationalTripFactInput.builder()
                .externalTripId("trip-2")
                .driverId("driver-1")
                .vehiclePlate(" abc-123 ")
                .build());

        assertEquals("ABC123", resolution.vehicleKey());
        assertEquals(OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_NORMALIZED_PLATE, resolution.vehicleKeySource());
    }

    @Test
    void resolveMarksNeedsReviewWhenVehicleDataMissing() {
        OperationalVehicleKeyResolver.Resolution resolution = resolver.resolve(OperationalTripFactInput.builder()
                .externalTripId("trip-3")
                .driverId("driver-1")
                .build());

        assertNull(resolution.vehicleKey());
        assertTrue(resolution.needsReview());
        assertEquals(OperationalMonitoringConstants.REASON_MISSING_VEHICLE_KEY, resolution.reviewReason());
    }
}
