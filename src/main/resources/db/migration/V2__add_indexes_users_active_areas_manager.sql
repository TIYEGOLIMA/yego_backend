-- Índices para acelerar listado de usuarios
-- users.active: filtrado por estado activo/inactivo
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);

-- areas.manager_id: búsqueda de responsable por usuario (findByManagerId)
CREATE INDEX IF NOT EXISTS idx_areas_manager_id ON areas(manager_id);
