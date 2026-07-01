-- ========================================
-- MÓDULO FLOTAS - Trazabilidad de cambios de flota (park) por vehículo
-- Migration 020
-- ========================================
-- Tabla append-only (solo INSERT) que registra:
--   INGRESO       -> primera vez que el vehículo aparece en el sistema (park de origen)
--   CAMBIO_FLOTA  -> cuando el vehículo cambia de una flota a otra

CREATE TABLE IF NOT EXISTS module_fleet_vehicle_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    yango_car_id        VARCHAR(255) NOT NULL,
    number              VARCHAR(50),
    segment_id_anterior UUID,
    segment_id_nuevo    UUID NOT NULL,
    park_id_anterior    VARCHAR(255),
    park_id_nuevo       VARCHAR(255) NOT NULL,
    tipo                VARCHAR(20) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_vehicle
        FOREIGN KEY (yango_car_id) REFERENCES module_fleet_vehicles (yango_car_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_history_segment_anterior
        FOREIGN KEY (segment_id_anterior) REFERENCES module_fleet_segments (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_history_segment_nuevo
        FOREIGN KEY (segment_id_nuevo) REFERENCES module_fleet_segments (id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_fleet_history_car     ON module_fleet_vehicle_history(yango_car_id);
CREATE INDEX IF NOT EXISTS idx_fleet_history_created ON module_fleet_vehicle_history(created_at);
