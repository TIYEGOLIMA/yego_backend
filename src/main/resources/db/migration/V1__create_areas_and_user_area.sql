-- Módulo de gestión de áreas
-- Ejecutar manualmente si ddl-auto=none (no hay Flyway/Liquibase automático)

-- Tabla de áreas (sin llaves foráneas en BD; solo IDs de referencia)
CREATE TABLE IF NOT EXISTS areas (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    manager_id BIGINT,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índice para búsqueda por nombre
CREATE UNIQUE INDEX IF NOT EXISTS idx_areas_name ON areas(name);

-- Columna area_id en users (nullable). Si ya existe, omitir esta línea.
ALTER TABLE users ADD COLUMN area_id BIGINT;

-- Índice para filtrar usuarios por área
CREATE INDEX IF NOT EXISTS idx_users_area_id ON users(area_id);

COMMENT ON TABLE areas IS 'Áreas organizacionales; cada una puede tener un responsable (manager_id)';
COMMENT ON COLUMN users.area_id IS 'Área a la que pertenece el usuario (colaborador)';
