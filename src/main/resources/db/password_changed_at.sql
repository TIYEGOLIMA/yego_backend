-- Añadir columna para política de cambio de contraseña semanal (Integral).
-- Ejecutar en la base de datos yego_integral (o la que use application.properties).

ALTER TABLE users
ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;

-- Opcional: para usuarios existentes, dar 7 días desde ahora (o usar last_login para no forzar de inmediato)
-- UPDATE users SET password_changed_at = COALESCE(last_login, created_at) WHERE password_changed_at IS NULL;

-- Si prefieres que todos deban cambiar en el próximo login, no ejecutes el UPDATE anterior.
-- Si prefieres dar 7 días a usuarios existentes:
UPDATE users SET password_changed_at = NOW() WHERE password_changed_at IS NULL;
