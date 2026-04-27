-- =============================================================================
-- MIGRACION: Yego Pro Ops pasa a lógica DIARIA por subturnos manana/tarde.
--            - Wipe de turnos calculados y cierres existentes (cambia el modelo
--              y el usuario pidió eliminar la data anterior).
--            - Reemplaza el CHECK constraint sobre tipo_turno
--              ('diurno'|'nocturno' -> 'manana'|'tarde').
--            - Garantiza UNIQUE(driver_id, fecha, tipo_turno) para upsert seguro.
-- Fecha:     2026-04-24
-- =============================================================================

BEGIN;

TRUNCATE TABLE module_driver_closes RESTART IDENTITY CASCADE;
TRUNCATE TABLE module_calculated_shifts RESTART IDENTITY CASCADE;

ALTER TABLE module_calculated_shifts
    DROP CONSTRAINT IF EXISTS module_calculated_shifts_tipo_turno_check;

ALTER TABLE module_calculated_shifts
    ADD CONSTRAINT module_calculated_shifts_tipo_turno_check
    CHECK (tipo_turno IN ('manana', 'tarde'));

CREATE UNIQUE INDEX IF NOT EXISTS uq_calc_shifts_driver_fecha_tipo
    ON module_calculated_shifts (driver_id, fecha, tipo_turno);

COMMIT;
