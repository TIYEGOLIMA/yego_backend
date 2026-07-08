-- ========================================
-- Bono Yango Lunes - Bonificación por cumplir objetivo
-- Migration 016
-- ========================================

ALTER TABLE module_weekly_billing ADD COLUMN IF NOT EXISTS bono_yango_lunes NUMERIC(12,2) DEFAULT 0;
