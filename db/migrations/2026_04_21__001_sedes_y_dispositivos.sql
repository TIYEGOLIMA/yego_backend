-- =============================================================================
-- MIGRACION: Sedes + Dispositivos para la Ticketera
-- Fecha:     2026-04-21
-- Autor:     YEGO
--
-- Esta migracion:
--   1. Crea la tabla `sedes` con dos sedes iniciales: San Miguel y Trujillo.
--   2. Crea la tabla `dispositivos` para tablets / TVs con auth propia (BCrypt).
--   3. Agrega la columna `sede_id` a `yego_modules` (modulos de atencion SAC).
--   4. Agrega la columna `sede_id` a `tickets`.
--   5. Quita las columnas redundantes `module_id` y `sede_id` de `users`
--      (si alguna vez existieron).
--   6. Desactiva los antiguos usuarios-tablet/TV de Iquitos (no se eliminan
--      para preservar integridad referencial con tickets historicos).
--
-- IMPORTANTE: Ejecutar en una transaccion. Hacer backup antes.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Tabla `sedes`
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sedes (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'America/Lima'),
    updated_at  TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_sedes_name_lower
    ON sedes (LOWER(name));

INSERT INTO sedes (name, description, active)
VALUES
    ('San Miguel', 'Sede San Miguel - Lima',  TRUE),
    ('Trujillo',   'Sede Trujillo - La Libertad', TRUE)
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- 2. Tabla `dispositivos`
--    Cada tablet / TV tiene su propia identidad y access_token (BCrypt hash).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dispositivos (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    type         VARCHAR(30)  NOT NULL
                 CHECK (type IN ('TABLET_PRINCIPAL', 'TABLET', 'TV')),
    sede_id      BIGINT       NOT NULL REFERENCES sedes (id),
    module_id    BIGINT       REFERENCES yego_modules (id),
    access_token VARCHAR(255) NOT NULL UNIQUE,
    description  TEXT,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'America/Lima'),
    updated_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_dispositivos_sede_active
    ON dispositivos (sede_id, active);

-- -----------------------------------------------------------------------------
-- 3. yego_modules: agregar sede_id (modulos pertenecen a una sede)
-- -----------------------------------------------------------------------------
ALTER TABLE yego_modules
    ADD COLUMN IF NOT EXISTS sede_id BIGINT REFERENCES sedes (id);

CREATE INDEX IF NOT EXISTS ix_yego_modules_sede
    ON yego_modules (sede_id);

-- -----------------------------------------------------------------------------
-- 4. tickets: agregar sede_id
-- -----------------------------------------------------------------------------
ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS sede_id BIGINT REFERENCES sedes (id);

CREATE INDEX IF NOT EXISTS ix_tickets_sede
    ON tickets (sede_id);

CREATE INDEX IF NOT EXISTS ix_tickets_sede_status_created
    ON tickets (sede_id, status, created_at);

-- -----------------------------------------------------------------------------
-- 5. users: quitar columnas redundantes (module_id, sede_id si existe)
--    `module_id` apuntaba a yego_modules y ahora vive en `dispositivos`.
--    `sede_id` se deriva del modulo asignado o del sedeId enviado por el front.
-- -----------------------------------------------------------------------------
ALTER TABLE users
    DROP COLUMN IF EXISTS module_id;

ALTER TABLE users
    DROP COLUMN IF EXISTS sede_id;

-- -----------------------------------------------------------------------------
-- 6. Desactivar antiguos usuarios-tablet/TV (Iquitos).
--    Se desactivan, NO se eliminan, para no romper FKs con tickets historicos.
--    Ajustar el WHERE si tus nombres / roles son distintos.
-- -----------------------------------------------------------------------------
UPDATE users u
SET    active = FALSE
WHERE  u.id IN (
    SELECT u2.id
    FROM   users u2
    LEFT   JOIN roles r ON r.id = u2.role_id
    WHERE  UPPER(COALESCE(r.name, '')) IN ('TABLET1', 'TABLET2', 'TV', 'TABLET_PRINCIPAL')
       OR  LOWER(u2.username) IN ('tv', 'tablet', 'tablet1', 'tablet2',
                                  'tabletprincipal', 'tablet_principal',
                                  'tablet principal', 'tabletppal')
);

-- -----------------------------------------------------------------------------
-- Verificacion rapida
-- -----------------------------------------------------------------------------
-- SELECT * FROM sedes;
-- SELECT id, name, sede_id FROM yego_modules;
-- SELECT COUNT(*) FROM tickets WHERE sede_id IS NULL;
-- SELECT id, username, active FROM users WHERE active = FALSE;

COMMIT;
