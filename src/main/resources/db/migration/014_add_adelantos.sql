-- ========================================
-- ADELANTOS - Avances a conductores
-- Migration 014
-- ========================================

-- Adelanto por sesión/cierre
ALTER TABLE module_driver_closes ADD COLUMN IF NOT EXISTS adelanto NUMERIC(10,2) DEFAULT 0;

-- Totales en liquidación semanal
ALTER TABLE module_weekly_billing ADD COLUMN IF NOT EXISTS total_adelantos NUMERIC(12,2) DEFAULT 0;
ALTER TABLE module_weekly_billing ADD COLUMN IF NOT EXISTS pago_total_con_adelantos NUMERIC(12,2);
