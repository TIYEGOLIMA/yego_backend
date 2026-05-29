ALTER TABLE module_calculated_shifts ADD COLUMN IF NOT EXISTS efectivo_total NUMERIC(12,2) DEFAULT 0;

DELETE FROM module_calculated_shifts WHERE fecha BETWEEN '2026-05-25' AND '2026-05-29';
