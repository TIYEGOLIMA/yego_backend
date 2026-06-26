-- ========================================
-- Operational automatic shift mirror - Phase 1A
-- Migration 017
-- ========================================

CREATE TABLE IF NOT EXISTS operational_trip_facts (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_trip_id            VARCHAR(255) NOT NULL,
    driver_id                   VARCHAR(255) NOT NULL,
    vehicle_key                 VARCHAR(255),
    vehicle_key_source          VARCHAR(32),
    vehicle_id                  VARCHAR(255),
    vehicle_plate               VARCHAR(32),
    vehicle_plate_normalized    VARCHAR(32),
    trip_status                 VARCHAR(64),
    booked_at                   TIMESTAMPTZ,
    ended_at                    TIMESTAMPTZ,
    observed_at                 TIMESTAMPTZ NOT NULL,
    source                      VARCHAR(32) NOT NULL DEFAULT 'YANGO',
    source_payload_hash         VARCHAR(128),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_operational_trip_facts_external_trip_id UNIQUE (external_trip_id)
);

CREATE TABLE IF NOT EXISTS operational_shift_sessions (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id                   VARCHAR(255) NOT NULL,
    vehicle_key                 VARCHAR(255),
    vehicle_key_source          VARCHAR(32),
    vehicle_id                  VARCHAR(255),
    vehicle_plate_normalized    VARCHAR(32),
    opened_at                   TIMESTAMPTZ NOT NULL,
    closed_at                   TIMESTAMPTZ,
    state                       VARCHAR(64) NOT NULL,
    open_reason                 VARCHAR(128) NOT NULL,
    close_reason                VARCHAR(128),
    first_trip_external_id      VARCHAR(255),
    last_trip_external_id       VARCHAR(255),
    trip_count                  INT NOT NULL DEFAULT 0,
    last_activity_at            TIMESTAMPTZ,
    confidence_level            VARCHAR(16),
    needs_review                BOOLEAN NOT NULL DEFAULT FALSE,
    review_reason               VARCHAR(128),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_operational_shift_state CHECK (
        state IN ('OPEN_INFERRED', 'AUTO_CLOSED_BY_NEXT_DRIVER', 'STALE_CANDIDATE', 'NEEDS_REVIEW')
    ),
    CONSTRAINT chk_operational_shift_confidence CHECK (
        confidence_level IS NULL OR confidence_level IN ('HIGH', 'MEDIUM', 'LOW')
    )
);

CREATE TABLE IF NOT EXISTS operational_shift_events (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operational_shift_session_id    UUID,
    event_type                      VARCHAR(64) NOT NULL,
    event_time                      TIMESTAMPTZ NOT NULL,
    driver_id                       VARCHAR(255),
    vehicle_key                     VARCHAR(255),
    external_trip_id                VARCHAR(255),
    reason                          VARCHAR(128),
    details                         TEXT,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_operational_trip_facts_driver_booked
    ON operational_trip_facts (driver_id, booked_at);
CREATE INDEX IF NOT EXISTS idx_operational_trip_facts_vehicle_key_booked
    ON operational_trip_facts (vehicle_key, booked_at);
CREATE INDEX IF NOT EXISTS idx_operational_trip_facts_plate_norm_booked
    ON operational_trip_facts (vehicle_plate_normalized, booked_at);
CREATE INDEX IF NOT EXISTS idx_operational_trip_facts_status_booked
    ON operational_trip_facts (trip_status, booked_at);
CREATE INDEX IF NOT EXISTS idx_operational_trip_facts_observed_at
    ON operational_trip_facts (observed_at);

CREATE INDEX IF NOT EXISTS idx_operational_shift_sessions_driver_opened
    ON operational_shift_sessions (driver_id, opened_at);
CREATE INDEX IF NOT EXISTS idx_operational_shift_sessions_vehicle_key_opened
    ON operational_shift_sessions (vehicle_key, opened_at);
CREATE INDEX IF NOT EXISTS idx_operational_shift_sessions_state_opened
    ON operational_shift_sessions (state, opened_at);
CREATE INDEX IF NOT EXISTS idx_operational_shift_sessions_review_opened
    ON operational_shift_sessions (needs_review, opened_at);
CREATE INDEX IF NOT EXISTS idx_operational_shift_sessions_last_activity
    ON operational_shift_sessions (last_activity_at);

CREATE INDEX IF NOT EXISTS idx_operational_shift_events_shift
    ON operational_shift_events (operational_shift_session_id);
CREATE INDEX IF NOT EXISTS idx_operational_shift_events_time
    ON operational_shift_events (event_time);
CREATE INDEX IF NOT EXISTS idx_operational_shift_events_driver
    ON operational_shift_events (driver_id, event_time);
CREATE INDEX IF NOT EXISTS idx_operational_shift_events_vehicle
    ON operational_shift_events (vehicle_key, event_time);
