-- Mobile Shift Integration
-- Agrega columnas NULLABLE a driver_closes para soportar la app móvil
-- Ejecutar en la BD de YEGO Pro Ops (MySQL/PostgreSQL)

ALTER TABLE module_driver_closes 
  ADD COLUMN IF NOT EXISTS car_photos TEXT NULL COMMENT 'JSON array URLs - fotos apertura mobile',
  ADD COLUMN IF NOT EXISTS selfie_uri VARCHAR(500) NULL COMMENT 'URL selfie conductor mobile',
  ADD COLUMN IF NOT EXISTS car_photos_cierre TEXT NULL COMMENT 'JSON array URLs - fotos cierre mobile',
  ADD COLUMN IF NOT EXISTS fotos_evidencia TEXT NULL COMMENT 'JSON array URLs - comprobantes gastos mobile',
  ADD COLUMN IF NOT EXISTS observaciones_apertura TEXT NULL COMMENT 'Observaciones al abrir turno mobile',
  ADD COLUMN IF NOT EXISTS observaciones_cierre TEXT NULL COMMENT 'Observaciones al cerrar turno mobile',
  ADD COLUMN IF NOT EXISTS mantenimiento_requerido BOOLEAN NULL COMMENT 'Requiere mantenimiento mobile',
  ADD COLUMN IF NOT EXISTS mantenimiento_descripcion TEXT NULL COMMENT 'Descripcion mantenimiento mobile',
  ADD COLUMN IF NOT EXISTS saldo_anterior DECIMAL(12,2) NULL COMMENT 'Saldo turno anterior mobile',
  ADD COLUMN IF NOT EXISTS saldo_descripcion VARCHAR(300) NULL COMMENT 'Descripcion saldo anterior mobile';

-- Nota: para PostgreSQL usar ADD COLUMN IF NOT EXISTS
-- Para MySQL, ejecutar cada ALTER por separado si la columna ya existe
