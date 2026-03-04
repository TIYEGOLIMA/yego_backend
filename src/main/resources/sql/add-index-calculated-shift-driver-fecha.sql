-- Índice para acelerar findByDriverIdAndFecha (GET /api/pro-ops/driver/calcular-turnos y otras consultas)
-- Ejecutar en PostgreSQL si la tabla ya existe y no usas ddl-auto=update
CREATE INDEX IF NOT EXISTS idx_calculated_shift_driver_fecha
ON module_calculated_shifts (driver_id, fecha);
