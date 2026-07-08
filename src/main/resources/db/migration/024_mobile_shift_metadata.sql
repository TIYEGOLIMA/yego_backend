-- ============================================================
-- Metadatos de turnos abiertos/cerrados desde app movil
-- ============================================================

ALTER TABLE shift_sessions
    ADD COLUMN IF NOT EXISTS vehicle_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS placa VARCHAR(30),
    ADD COLUMN IF NOT EXISTS modelo VARCHAR(255),
    ADD COLUMN IF NOT EXISTS km_inicial INT,
    ADD COLUMN IF NOT EXISTS km_final INT,
    ADD COLUMN IF NOT EXISTS selfie_uri TEXT,
    ADD COLUMN IF NOT EXISTS car_photos TEXT,
    ADD COLUMN IF NOT EXISTS car_photos_cierre TEXT,
    ADD COLUMN IF NOT EXISTS observaciones TEXT,
    ADD COLUMN IF NOT EXISTS mantenimiento_requerido BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS mantenimiento_descripcion TEXT;

CREATE INDEX IF NOT EXISTS idx_shift_sessions_placa ON shift_sessions (placa);
CREATE INDEX IF NOT EXISTS idx_shift_sessions_vehicle ON shift_sessions (vehicle_id);
