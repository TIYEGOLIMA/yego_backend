-- Prueba: hacer que el usuario id 26 vea el modal de cambio de contraseña al hacer login.
-- Opción A: poner la última fecha de cambio hace 8 días (así han pasado 7+ días)
UPDATE users SET password_changed_at = NOW() - INTERVAL '8 days' WHERE id = 26;

-- Opción B (alternativa): si la columna es NULL también se exige cambio
-- UPDATE users SET password_changed_at = NULL WHERE id = 26;
