-- WorkOS / Yego Gantt – chat por tarea (Fase 1). Aplicar manualmente en PostgreSQL en orden.
-- Tabla: mensajes de hilo por tarea / subtarea.

CREATE TABLE IF NOT EXISTS workos_task_messages (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL,
    subtask_id      BIGINT NULL,
    author_user_id  BIGINT NULL,
    message_type    VARCHAR(30) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NULL,
    deleted_at      TIMESTAMP NULL,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_workos_task_messages_task
        FOREIGN KEY (task_id) REFERENCES area_tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_workos_task_messages_subtask
        FOREIGN KEY (subtask_id) REFERENCES area_task_subtasks (id) ON DELETE SET NULL,
    CONSTRAINT fk_workos_task_messages_author
        FOREIGN KEY (author_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_workos_task_messages_message_type
        CHECK (message_type IN ('USER', 'SYSTEM', 'RESOLUTION'))
);

CREATE INDEX IF NOT EXISTS idx_workos_task_messages_task_id_created_at
    ON workos_task_messages (task_id, created_at);

CREATE INDEX IF NOT EXISTS idx_workos_task_messages_subtask_id_created_at
    ON workos_task_messages (subtask_id, created_at)
    WHERE subtask_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_workos_task_messages_author_user_id
    ON workos_task_messages (author_user_id);
