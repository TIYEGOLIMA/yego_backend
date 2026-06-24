-- ============================================================
-- Agregar monto_total_producido a module_driver_closes
-- (producido bruto Yego Pro: tarjeta + efectivo + propinas/promos)
-- PostgreSQL
-- ============================================================

ALTER TABLE module_driver_closes
  ADD COLUMN IF NOT EXISTS monto_total_producido NUMERIC(12,2);
