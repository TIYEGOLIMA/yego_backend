-- ============================================================
-- Agregar shift_session_id a la tabla de cierres de conductor
-- PostgreSQL
-- ============================================================

ALTER TABLE module_driver_closes
  ADD COLUMN IF NOT EXISTS shift_session_id UUID;

CREATE INDEX IF NOT EXISTS idx_driver_close_shift_session
  ON module_driver_closes(shift_session_id);
