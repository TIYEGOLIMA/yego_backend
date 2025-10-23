-- Agregar columna notes a la tabla module_attendance_records
ALTER TABLE module_attendance_records ADD COLUMN IF NOT EXISTS notes TEXT;
