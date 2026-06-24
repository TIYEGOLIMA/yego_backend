-- ========================================
-- Validación de sesiones - Workflow
-- Migration 015
-- ========================================

-- Cambiar valor por defecto de status
ALTER TABLE shift_sessions ALTER COLUMN status SET DEFAULT 'por_validar';
