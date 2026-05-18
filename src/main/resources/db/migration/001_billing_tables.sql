-- ============================================================
-- Tablas de facturación semanal + configuración de bonos
-- PostgreSQL
-- ============================================================

CREATE TABLE IF NOT EXISTS module_weekly_billing (
    id                      BIGSERIAL PRIMARY KEY,
    driver_id               VARCHAR(255)    NOT NULL,
    fecha_inicio            DATE            NOT NULL,
    fecha_fin               DATE            NOT NULL,
    total_viajes            INT,
    viajes_validos          INT,
    horas_trabajo           DOUBLE PRECISION,
    monto_total_producido   NUMERIC(12,2),
    comision_app            NUMERIC(12,2),
    monto_neto              NUMERIC(12,2),
    km_recorrido            NUMERIC(10,2),
    gasto_combustible       NUMERIC(10,2),
    gasto_mantenimiento     NUMERIC(10,2),
    produccion_bonificable  NUMERIC(12,2),
    bono_adic_viajes        NUMERIC(10,2),
    bono                    NUMERIC(12,2),
    porcentaje_pago         DOUBLE PRECISION,
    pago                    NUMERIC(12,2),
    descuento_yego          NUMERIC(10,2),
    pago_total              NUMERIC(12,2),
    utilidad                NUMERIC(12,2),
    utilidad_por_viaje      NUMERIC(10,2),
    pago_por_viaje          NUMERIC(10,2),
    dias_trabajados         INT,
    dias_liquidados         INT,
    turno                   VARCHAR(10),
    estado                  VARCHAR(15)     NOT NULL DEFAULT 'pendiente',
    user_id                 BIGINT,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS module_bonus_thresholds (
    id              BIGSERIAL PRIMARY KEY,
    min_trips       INT             NOT NULL,
    bonus_amount    NUMERIC(10,2)   NOT NULL,
    effective_from  DATE            NOT NULL,
    updated_by      BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bonus_trip_date UNIQUE (min_trips, effective_from)
);

CREATE TABLE IF NOT EXISTS module_payment_percentages (
    id                      BIGSERIAL PRIMARY KEY,
    min_validated_trips     INT                 NOT NULL,
    percentage              DOUBLE PRECISION    NOT NULL,
    effective_from          DATE                NOT NULL,
    updated_by              BIGINT,
    created_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pct_trip_date UNIQUE (min_validated_trips, effective_from)
);

-- ============================================================
-- Índices
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_weekly_driver      ON module_weekly_billing (driver_id);
CREATE INDEX IF NOT EXISTS idx_weekly_fechas      ON module_weekly_billing (fecha_inicio, fecha_fin);
CREATE INDEX IF NOT EXISTS idx_weekly_driver_fecha ON module_weekly_billing (driver_id, fecha_inicio DESC);
CREATE INDEX IF NOT EXISTS idx_weekly_estado       ON module_weekly_billing (estado);
CREATE INDEX IF NOT EXISTS idx_bonus_effective     ON module_bonus_thresholds (effective_from, min_trips DESC);
CREATE INDEX IF NOT EXISTS idx_pct_effective       ON module_payment_percentages (effective_from, min_validated_trips DESC);

-- Índice crítico para resumen-semanal: busca turnos por rango de fechas
CREATE INDEX IF NOT EXISTS idx_calculated_shift_fecha ON module_calculated_shifts (fecha, driver_id);

-- Índice crítico para resumen-semanal: busca cierres por rango de fechas
CREATE INDEX IF NOT EXISTS idx_driver_close_fecha  ON module_driver_closes (fecha, driver_id);

-- ============================================================
-- Datos iniciales (del Excel) - idempotente con ON CONFLICT
-- ============================================================

INSERT INTO module_bonus_thresholds (min_trips, bonus_amount, effective_from) VALUES
    (165, 300.00, '2024-05-01'),
    (150, 115.00, '2024-05-01'),
    (135, 100.00, '2024-05-01'),
    (125,  50.00, '2024-05-01')
ON CONFLICT (min_trips, effective_from) DO NOTHING;

INSERT INTO module_payment_percentages (min_validated_trips, percentage, effective_from) VALUES
    (140, 0.60, '2024-05-01'),
    (128, 0.55, '2024-05-01'),
    (117, 0.50, '2024-05-01'),
    (107, 0.45, '2024-05-01'),
    (100, 0.40, '2024-05-01'),
    ( 95, 0.35, '2024-05-01'),
    ( 90, 0.30, '2024-05-01')
ON CONFLICT (min_validated_trips, effective_from) DO NOTHING;
