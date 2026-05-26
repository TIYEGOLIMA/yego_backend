-- Cache del income card de Yango por conductor y semana.
-- Se persiste para no recargar del Fleet cada vez que se abre la facturación semanal.

CREATE TABLE IF NOT EXISTS module_weekly_income (
    id                      BIGSERIAL PRIMARY KEY,
    driver_id               VARCHAR(255)    NOT NULL,
    fecha_inicio            DATE            NOT NULL,
    fecha_fin               DATE            NOT NULL,
    bonificacion            NUMERIC(12,2),
    cash_collected          NUMERIC(12,2),
    non_cash_payment        NUMERIC(12,2),
    corporate               NUMERIC(12,2),
    tips                    NUMERIC(12,2),
    promotion_compensation  NUMERIC(12,2),
    platform_fees           NUMERIC(12,2),
    total                   NUMERIC(12,2),
    price_yango_pro         NUMERIC(12,2),
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_weekly_income UNIQUE (driver_id, fecha_inicio, fecha_fin)
);

CREATE INDEX IF NOT EXISTS idx_weekly_income_driver ON module_weekly_income (driver_id);
CREATE INDEX IF NOT EXISTS idx_weekly_income_fechas ON module_weekly_income (fecha_inicio, fecha_fin);
