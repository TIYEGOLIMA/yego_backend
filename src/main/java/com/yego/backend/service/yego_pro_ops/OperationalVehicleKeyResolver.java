package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.OperationalTripFactInput;

public interface OperationalVehicleKeyResolver {

    Resolution resolve(OperationalTripFactInput input);

    String normalizePlate(String plate);

    record Resolution(
            String vehicleKey,
            String vehicleKeySource,
            String vehicleId,
            String originalPlate,
            String normalizedPlate,
            boolean needsReview,
            String reviewReason) {
    }
}
