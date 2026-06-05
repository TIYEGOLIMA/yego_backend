-- ============================================================
-- Tablas de sesiones de turno por franja horaria
-- PostgreSQL
-- ============================================================

CREATE TABLE IF NOT EXISTS shift_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id       VARCHAR(255)     NOT NULL,
    started_at      TIMESTAMPTZ      NOT NULL,
    closed_at       TIMESTAMPTZ,
    settled_at      TIMESTAMPTZ,
    status          TEXT             NOT NULL DEFAULT 'active',
    total_trips     INT              DEFAULT 0,
    total_amount    NUMERIC(12,2)    DEFAULT 0,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_shift_sessions_status CHECK (status IN ('active', 'closed', 'settled'))
);

CREATE TABLE IF NOT EXISTS trips (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id           VARCHAR(255)     NOT NULL,
    shift_session_id    UUID             NOT NULL REFERENCES shift_sessions(id),
    external_trip_id    VARCHAR(255),
    completed_at        TIMESTAMPTZ      NOT NULL,
    amount              NUMERIC(12,2)    DEFAULT 0,
    created_at          TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Índices
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_shift_sessions_driver         ON shift_sessions (driver_id);
CREATE INDEX IF NOT EXISTS idx_shift_sessions_status          ON shift_sessions (status);
CREATE INDEX IF NOT EXISTS idx_shift_sessions_driver_status   ON shift_sessions (driver_id, status);
CREATE INDEX IF NOT EXISTS idx_shift_sessions_started         ON shift_sessions (driver_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_trips_session                  ON trips (shift_session_id);
CREATE INDEX IF NOT EXISTS idx_trips_driver                   ON trips (driver_id);
CREATE INDEX IF NOT EXISTS idx_trips_external                  ON trips (external_trip_id);
