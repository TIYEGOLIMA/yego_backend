-- =============================================================================
-- MIGRACION: Agrega columna token_version a dispositivos para invalidar sesiones
--            (JWT) cuando se regenera el token de emparejamiento.
-- Fecha:     2026-04-22
-- =============================================================================

BEGIN;

ALTER TABLE dispositivos
    ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;

COMMIT;
