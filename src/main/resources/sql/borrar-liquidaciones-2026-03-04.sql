-- Borrar liquidaciones (cierres) aprobadas del 4 de marzo de 2026
-- Ejecutar en PostgreSQL. Hacer backup antes si es necesario.

-- 1) Desmarcar turnos del 4 de marzo como pagados (para que vuelvan a "pendientes")
UPDATE module_calculated_shifts
SET pagado = false
WHERE fecha = '2026-03-04';

-- 2) Eliminar los registros de cierre de caja (liquidación aprobada) del 4 de marzo
DELETE FROM module_driver_closes
WHERE fecha = '2026-03-04';

-- Ver cuántos se afectaron (opcional, comentar si no usas):
-- SELECT COUNT(*) FROM module_calculated_shifts WHERE fecha = '2026-03-04';
-- SELECT COUNT(*) FROM module_driver_closes WHERE fecha = '2026-03-04';
