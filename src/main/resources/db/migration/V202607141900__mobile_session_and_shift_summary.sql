CREATE TABLE IF NOT EXISTS mobile_driver_sessions (
    driver_id VARCHAR(255) PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE,
    device_id VARCHAR(160) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mobile_driver_sessions_expires_at
    ON mobile_driver_sessions (expires_at);

ALTER TABLE shift_sessions
    ADD COLUMN IF NOT EXISTS total_yape NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_card NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_corporate NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_tips NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_bonus NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_promotion NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_distance NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS average_per_trip NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS summary_snapshot_saved BOOLEAN DEFAULT FALSE;
