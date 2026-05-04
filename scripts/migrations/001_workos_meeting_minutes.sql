-- Actas de reunión WorkOS (aditivo). Aplicar manualmente en PostgreSQL si ddl-auto=none en prod.
-- Convención API: /api/yego-gantt/meeting-minutes
-- Decisión: sin Flyway en el repo; script versionado para operaciones/CI.

CREATE TABLE IF NOT EXISTS workos_meeting_minutes (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    meeting_date DATE NOT NULL,
    meeting_type VARCHAR(50),
    summary TEXT,
    created_by_user_id BIGINT REFERENCES users (id),
    owner_user_id BIGINT REFERENCES users (id),
    status VARCHAR(50) NOT NULL DEFAULT 'ABIERTA',
    next_meeting_date DATE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wmm_meeting_date ON workos_meeting_minutes (meeting_date);
CREATE INDEX IF NOT EXISTS idx_wmm_status ON workos_meeting_minutes (status);
CREATE INDEX IF NOT EXISTS idx_wmm_owner ON workos_meeting_minutes (owner_user_id);

CREATE TABLE IF NOT EXISTS workos_meeting_minute_items (
    id BIGSERIAL PRIMARY KEY,
    meeting_minute_id BIGINT NOT NULL REFERENCES workos_meeting_minutes (id) ON DELETE CASCADE,
    item_order INT NOT NULL,
    area_id BIGINT REFERENCES areas (id),
    area_name_snapshot VARCHAR(255),
    project_id BIGINT REFERENCES gantt_projects (id),
    sprint_id BIGINT REFERENCES workos_sprints (id),
    item_type VARCHAR(50) NOT NULL DEFAULT 'ACCION',
    situation TEXT,
    decision TEXT,
    task_title VARCHAR(255),
    task_description TEXT,
    responsible_user_id BIGINT REFERENCES users (id),
    responsible_name_snapshot VARCHAR(255),
    start_date DATE,
    deadline DATE,
    priority VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
    converted_task_id BIGINT REFERENCES area_tasks (id),
    converted_at TIMESTAMP,
    converted_by_user_id BIGINT REFERENCES users (id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wmmi_minute_order UNIQUE (meeting_minute_id, item_order)
);

CREATE INDEX IF NOT EXISTS idx_wmmi_minute ON workos_meeting_minute_items (meeting_minute_id);
CREATE INDEX IF NOT EXISTS idx_wmmi_converted_task ON workos_meeting_minute_items (converted_task_id);
CREATE INDEX IF NOT EXISTS idx_wmmi_area ON workos_meeting_minute_items (area_id);

-- Un mismo id de tarea no puede vincularse a dos ítems de acta.
CREATE UNIQUE INDEX IF NOT EXISTS uq_wmmi_converted_task_id
    ON workos_meeting_minute_items (converted_task_id)
    WHERE converted_task_id IS NOT NULL;
