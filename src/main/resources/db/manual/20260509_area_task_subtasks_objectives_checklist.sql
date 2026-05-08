-- Objetivos y checklist interno por subtarea (JSON en checklist_json).
ALTER TABLE area_task_subtasks ADD COLUMN IF NOT EXISTS objectives VARCHAR(4000);
ALTER TABLE area_task_subtasks ADD COLUMN IF NOT EXISTS checklist_json TEXT;
