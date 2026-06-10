-- ============================================================
-- Agregar total_cash a shift_sessions (efectivo recolectado)
-- PostgreSQL
-- ============================================================

ALTER TABLE shift_sessions
  ADD COLUMN IF NOT EXISTS total_cash NUMERIC(12,2) DEFAULT 0;
