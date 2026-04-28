-- Icono visual del proyecto (clave, p. ej. folder, rocket). Ejecutar en PostgreSQL.
ALTER TABLE gantt_projects ADD COLUMN IF NOT EXISTS icon_key VARCHAR(40) NOT NULL DEFAULT 'folder';
