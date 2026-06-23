-- ========================================
-- MÓDULO FLOTAS - Tablas custom locales
-- Migration 012
-- ========================================

-- Eliminar tabla obsoleta si existe
DROP TABLE IF EXISTS module_vehicles CASCADE;

-- 1. Documentos del vehículo (custom, no viene de Yango)
CREATE TABLE IF NOT EXISTS module_vehicle_documents (
    id              BIGSERIAL PRIMARY KEY,
    yango_car_id    VARCHAR(255) NOT NULL,
    tipo            VARCHAR(50) NOT NULL,
    nombre          VARCHAR(200),
    fecha_inicio    DATE,
    fecha_fin       DATE,
    archivo_url     TEXT,
    estado          VARCHAR(20) NOT NULL DEFAULT 'vigente',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicle_docs_car ON module_vehicle_documents(yango_car_id);
CREATE INDEX idx_vehicle_docs_tipo ON module_vehicle_documents(tipo);
CREATE INDEX idx_vehicle_docs_fecha_fin ON module_vehicle_documents(fecha_fin);

-- 2. Mantenimiento (preventivo + correctivo)
CREATE TABLE IF NOT EXISTS module_vehicle_maintenance (
    id              BIGSERIAL PRIMARY KEY,
    yango_car_id    VARCHAR(255) NOT NULL,
    tipo            VARCHAR(20) NOT NULL DEFAULT 'preventivo',
    categoria       VARCHAR(100),
    fecha           DATE NOT NULL,
    kilometraje     NUMERIC(12,2),
    descripcion     TEXT,
    problema        TEXT,
    diagnostico     TEXT,
    solucion         TEXT,
    taller          VARCHAR(200),
    responsable     VARCHAR(200),
    costo           NUMERIC(10,2) DEFAULT 0,
    archivo_url     TEXT,
    estado          VARCHAR(20) NOT NULL DEFAULT 'completado',
    proxima_fecha   DATE,
    proximo_km      NUMERIC(12,2),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicle_maint_car ON module_vehicle_maintenance(yango_car_id);
CREATE INDEX idx_vehicle_maint_tipo ON module_vehicle_maintenance(tipo);
CREATE INDEX idx_vehicle_maint_fecha ON module_vehicle_maintenance(fecha);

-- 3. Historial de kilometraje
CREATE TABLE IF NOT EXISTS module_vehicle_mileage (
    id              BIGSERIAL PRIMARY KEY,
    yango_car_id    VARCHAR(255) NOT NULL,
    fecha           DATE NOT NULL,
    kilometraje     NUMERIC(12,2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicle_mileage_car ON module_vehicle_mileage(yango_car_id);
CREATE INDEX idx_vehicle_mileage_fecha ON module_vehicle_mileage(fecha);

-- 4. Siniestros
CREATE TABLE IF NOT EXISTS module_vehicle_incidents (
    id              BIGSERIAL PRIMARY KEY,
    yango_car_id    VARCHAR(255) NOT NULL,
    fecha           DATE NOT NULL,
    tipo            VARCHAR(100) NOT NULL,
    descripcion     TEXT,
    conductor       VARCHAR(200),
    monto_dano      NUMERIC(10,2) DEFAULT 0,
    estado          VARCHAR(20) NOT NULL DEFAULT 'reportado',
    evidencias      TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicle_incidents_car ON module_vehicle_incidents(yango_car_id);
CREATE INDEX idx_vehicle_incidents_estado ON module_vehicle_incidents(estado);
