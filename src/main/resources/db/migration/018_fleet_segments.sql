-- ========================================
-- MÓDULO FLOTAS - Segmentación por park_id + cache de vehículos
-- Migration 018
-- ========================================

-- Requerido para gen_random_uuid() en PostgreSQL 12.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Flotas registradas (segmento / categoría). Se identifica por park_id.
CREATE TABLE IF NOT EXISTS module_fleet_segments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    park_id       VARCHAR(255) NOT NULL UNIQUE,
    activo        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_fleet_segments_activo ON module_fleet_segments(activo);

-- 2. Vehículos cacheados de cada flota (se sincronizan desde Yango).
--    park_id NO se duplica aquí: el vehículo referencia la flota por segment_id (FK a la PK).
CREATE TABLE IF NOT EXISTS module_fleet_vehicles (
    yango_car_id  VARCHAR(255) PRIMARY KEY,
    segment_id    UUID NOT NULL,
    number        VARCHAR(50),
    brand         VARCHAR(100),
    model         VARCHAR(100),
    year          INTEGER,
    color         VARCHAR(50),
    color_name    VARCHAR(100),
    vin           VARCHAR(100),
    callsign      VARCHAR(100),
    status_id     VARCHAR(50),
    status_name   VARCHAR(100),
    categories    TEXT,
    amenities     TEXT,
    mileage       INTEGER,
    rental        BOOLEAN,
    foto_url      TEXT,
    activo        BOOLEAN NOT NULL DEFAULT TRUE,
    synced_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fleet_vehicles_segment
        FOREIGN KEY (segment_id) REFERENCES module_fleet_segments (id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_fleet_vehicles_segment ON module_fleet_vehicles(segment_id);
CREATE INDEX IF NOT EXISTS idx_fleet_vehicles_number  ON module_fleet_vehicles(number);
CREATE INDEX IF NOT EXISTS idx_fleet_vehicles_activo  ON module_fleet_vehicles(activo);

-- 3. Seed de flotas por defecto (idempotente). Las demás se agregan desde la UI.
INSERT INTO module_fleet_segments (park_id, activo)
VALUES
    ('64085dd85e124e2c808806f70d527ea8', TRUE), -- Yego Pro
    ('fafd623109d740f8a1f15af7c3dd86c6', TRUE)  -- Yegó mi auto
ON CONFLICT (park_id) DO NOTHING;
