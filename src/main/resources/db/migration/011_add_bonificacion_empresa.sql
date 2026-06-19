ALTER TABLE module_weekly_billing ADD COLUMN IF NOT EXISTS bonificacion_empresa NUMERIC(12,2);
ALTER TABLE module_weekly_billing ADD COLUMN IF NOT EXISTS pago_total_final NUMERIC(12,2);
