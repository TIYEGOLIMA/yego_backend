-- ========================================
-- MÓDULO FLOTAS - Documentos: fecha_vigente (reemplaza fecha_inicio/fecha_fin)
-- Migration 022
-- ========================================

ALTER TABLE module_vehicle_documents ADD COLUMN IF NOT EXISTS fecha_vigente DATE;
ALTER TABLE module_vehicle_documents DROP COLUMN IF EXISTS fecha_inicio;
ALTER TABLE module_vehicle_documents DROP COLUMN IF EXISTS fecha_fin;
