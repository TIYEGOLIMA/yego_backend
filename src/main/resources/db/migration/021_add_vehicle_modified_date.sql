-- ========================================
-- MÓDULO FLOTAS - modified_date para detección de cambios (foto)
-- Migration 021
-- ========================================
-- Guarda el modified_date que devuelve Yango para refrescar la foto solo
-- cuando el vehículo cambió (fetch-if-missing-or-changed).

ALTER TABLE module_fleet_vehicles ADD COLUMN IF NOT EXISTS modified_date VARCHAR(50);
