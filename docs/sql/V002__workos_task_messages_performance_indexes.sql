-- Índices adicionales para listados rápidos de mensajes (GET con y sin subtaskId).
-- Aplicar tras V001 si el volumen crece y el endpoint se percibe lento.

-- Listado por tarea (solo filas activas): evita barrer registros borrados.
CREATE INDEX IF NOT EXISTS idx_workos_task_messages_task_created_active
    ON workos_task_messages (task_id, created_at ASC)
    WHERE is_deleted = FALSE;

-- Listado por tarea + subtarea (hilo por subtarea).
CREATE INDEX IF NOT EXISTS idx_workos_task_messages_task_subtask_created_active
    ON workos_task_messages (task_id, subtask_id, created_at ASC)
    WHERE is_deleted = FALSE AND subtask_id IS NOT NULL;
