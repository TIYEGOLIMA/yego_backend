-- Script para corregir las columnas ip_address de tipo inet a varchar
-- Ejecutar en PostgreSQL para resolver errores de tipo de datos

BEGIN;

-- Cambiar columna ip_address en tabla sessions de inet a varchar
ALTER TABLE sessions ALTER COLUMN ip_address TYPE varchar(45) USING ip_address::text;

-- Cambiar columna ip_address en tabla audit_logs de inet a varchar  
ALTER TABLE audit_logs ALTER COLUMN ip_address TYPE varchar(45) USING ip_address::text;

-- Cambiar columna ip_address en tabla connection_logs de inet a varchar
ALTER TABLE connection_logs ALTER COLUMN ip_address TYPE varchar(45) USING ip_address::text;

COMMIT;

-- Verificar los cambios
SELECT 
    table_name, 
    column_name, 
    data_type 
FROM information_schema.columns 
WHERE table_name IN ('sessions', 'audit_logs', 'connection_logs') 
  AND column_name = 'ip_address';
