-- ========================================
-- Validación de sesiones - Workflow
-- Migration 015
-- ========================================

-- Eliminar CHECK constraint anterior
ALTER TABLE shift_sessions DROP CONSTRAINT IF EXISTS chk_shift_sessions_status;

-- Crear nuevo CHECK constraint con todos los estados
ALTER TABLE shift_sessions ADD CONSTRAINT chk_shift_sessions_status 
  CHECK (status IN ('active', 'por_validar', 'completada', 'rechazada', 'settled'));

-- Cambiar valor por defecto de status
ALTER TABLE shift_sessions ALTER COLUMN status SET DEFAULT 'por_validar';
