-- Agrega columna bono_yango a la tabla de facturación semanal
-- para registrar el bono de plataforma Yango usado en el cálculo.

ALTER TABLE module_weekly_billing
    ADD COLUMN IF NOT EXISTS bono_yango NUMERIC(12,2);
