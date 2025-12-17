-- Script para agregar la columna es_manual a la tabla module_calculated_shifts
-- Esta columna indica si el turno fue creado manualmente por el usuario

ALTER TABLE module_calculated_shifts 
ADD COLUMN IF NOT EXISTS es_manual BOOLEAN NOT NULL DEFAULT FALSE;

-- Actualizar los registros existentes para que tengan es_manual = false (turnos automáticos)
UPDATE module_calculated_shifts 
SET es_manual = FALSE 
WHERE es_manual IS NULL;

