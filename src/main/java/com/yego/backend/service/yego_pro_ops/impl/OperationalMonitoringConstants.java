package com.yego.backend.service.yego_pro_ops.impl;

public final class OperationalMonitoringConstants {

    private OperationalMonitoringConstants() {
    }

    public static final String SOURCE_YANGO = "YANGO";

    public static final String VEHICLE_KEY_SOURCE_YANGO_CAR_ID = "YANGO_CAR_ID";
    public static final String VEHICLE_KEY_SOURCE_NORMALIZED_PLATE = "NORMALIZED_PLATE";
    public static final String VEHICLE_KEY_SOURCE_UNRESOLVED = "UNRESOLVED";

    public static final String CONFIDENCE_HIGH = "HIGH";
    public static final String CONFIDENCE_MEDIUM = "MEDIUM";
    public static final String CONFIDENCE_LOW = "LOW";

    public static final String STATE_OPEN_INFERRED = "OPEN_INFERRED";
    public static final String STATE_AUTO_CLOSED_BY_NEXT_DRIVER = "AUTO_CLOSED_BY_NEXT_DRIVER";
    public static final String STATE_STALE_CANDIDATE = "STALE_CANDIDATE";
    public static final String STATE_NEEDS_REVIEW = "NEEDS_REVIEW";

    public static final String EVENT_TRIP_FACT_UPSERTED = "TRIP_FACT_UPSERTED";
    public static final String EVENT_SHIFT_OPENED = "SHIFT_OPENED";
    public static final String EVENT_SHIFT_CONTINUED = "SHIFT_CONTINUED";
    public static final String EVENT_SHIFT_AUTO_CLOSED_BY_NEXT_DRIVER = "SHIFT_AUTO_CLOSED_BY_NEXT_DRIVER";
    public static final String EVENT_SHIFT_MARKED_STALE_CANDIDATE = "SHIFT_MARKED_STALE_CANDIDATE";
    public static final String EVENT_SHIFT_NEEDS_REVIEW = "SHIFT_NEEDS_REVIEW";
    public static final String EVENT_LATE_TRIP_DETECTED = "LATE_TRIP_DETECTED";
    public static final String EVENT_MISSING_VEHICLE_KEY = "MISSING_VEHICLE_KEY";

    public static final String REASON_FIRST_COMPLETED_TRIP = "FIRST_COMPLETED_TRIP";
    public static final String REASON_NEXT_DRIVER_ACTIVITY = "NEXT_DRIVER_ACTIVITY";
    public static final String REASON_STALE_THRESHOLD_EXCEEDED = "STALE_THRESHOLD_EXCEEDED";
    public static final String REASON_MISSING_VEHICLE_KEY = "MISSING_VEHICLE_KEY";
    public static final String REASON_SAME_DRIVER_ACTIVITY = "SAME_DRIVER_ACTIVITY";
    public static final String REASON_FALLBACK_ENDED_AT = "FALLBACK_ENDED_AT";
}
