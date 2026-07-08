-- ============================================================
-- Metadatos moviles de apertura/cierre en cierres de conductor
-- ============================================================

ALTER TABLE module_driver_closes
    ADD COLUMN IF NOT EXISTS car_photos TEXT,
    ADD COLUMN IF NOT EXISTS selfie_uri VARCHAR(500),
    ADD COLUMN IF NOT EXISTS car_photos_cierre TEXT,
    ADD COLUMN IF NOT EXISTS fotos_evidencia TEXT,
    ADD COLUMN IF NOT EXISTS observaciones_apertura TEXT,
    ADD COLUMN IF NOT EXISTS observaciones_cierre TEXT,
    ADD COLUMN IF NOT EXISTS mantenimiento_requerido BOOLEAN,
    ADD COLUMN IF NOT EXISTS mantenimiento_descripcion TEXT,
    ADD COLUMN IF NOT EXISTS saldo_anterior NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS saldo_descripcion VARCHAR(300);
