-- Estado Kanban por subtarea (independiente del proyecto padre).
ALTER TABLE area_task_subtasks ADD COLUMN IF NOT EXISTS kanban_status VARCHAR(32) NOT NULL DEFAULT 'PENDING';
UPDATE area_task_subtasks SET kanban_status = 'DONE' WHERE done = true AND kanban_status = 'PENDING';
