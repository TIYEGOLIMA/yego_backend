-- ========================================
-- MÓDULO FLOTAS - Auditoría de documentos (soft delete + quién cargó/eliminó)
-- Migration 023
-- ========================================

ALTER TABLE module_vehicle_documents ADD COLUMN IF NOT EXISTS created_by_id BIGINT;
ALTER TABLE module_vehicle_documents ADD COLUMN IF NOT EXISTS eliminado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE module_vehicle_documents ADD COLUMN IF NOT EXISTS deleted_by_id BIGINT;
ALTER TABLE module_vehicle_documents ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_vehicle_docs_eliminado ON module_vehicle_documents(eliminado);
