package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.request.OperationalTripFactInput;
import com.yego.backend.service.yego_pro_ops.OperationalVehicleKeyResolver;
import org.springframework.stereotype.Service;

@Service
public class OperationalVehicleKeyResolverImpl implements OperationalVehicleKeyResolver {

    @Override
    public Resolution resolve(OperationalTripFactInput input) {
        String normalizedPlate = normalizePlate(input.getVehiclePlate());
        String explicitVehicleKey = clean(input.getVehicleKey());
        String vehicleId = clean(input.getVehicleId());

        if (explicitVehicleKey != null) {
            String source = clean(input.getVehicleKeySource());
            if (source == null) {
                source = vehicleId != null && vehicleId.equals(explicitVehicleKey)
                        ? OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_YANGO_CAR_ID
                        : OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_NORMALIZED_PLATE;
            }
            return new Resolution(
                    explicitVehicleKey,
                    source,
                    vehicleId,
                    clean(input.getVehiclePlate()),
                    normalizedPlate,
                    false,
                    null);
        }

        if (vehicleId != null) {
            return new Resolution(
                    vehicleId,
                    OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_YANGO_CAR_ID,
                    vehicleId,
                    clean(input.getVehiclePlate()),
                    normalizedPlate,
                    false,
                    null);
        }

        if (normalizedPlate != null) {
            return new Resolution(
                    normalizedPlate,
                    OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_NORMALIZED_PLATE,
                    null,
                    clean(input.getVehiclePlate()),
                    normalizedPlate,
                    false,
                    null);
        }

        return new Resolution(
                null,
                OperationalMonitoringConstants.VEHICLE_KEY_SOURCE_UNRESOLVED,
                null,
                clean(input.getVehiclePlate()),
                null,
                true,
                OperationalMonitoringConstants.REASON_MISSING_VEHICLE_KEY);
    }

    @Override
    public String normalizePlate(String plate) {
        String cleaned = clean(plate);
        if (cleaned == null) {
            return null;
        }
        String normalized = cleaned
                .trim()
                .toUpperCase()
                .replaceAll("[\\s\\-_/.:]", "");
        return normalized.isBlank() ? null : normalized;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
