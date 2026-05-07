-- Ejemplo: borrar tarea id 39 y datos ligados (ajústalo si otro id).
-- Preferible usar DELETE en la API (`/api/yego-gantt/tasks/39`) tras el fix de cascada en código.

UPDATE workos_meeting_minute_items
SET converted_task_id = NULL, converted_at = NULL, converted_by_user_id = NULL
WHERE converted_task_id = 39;

DELETE FROM workos_task_messages WHERE task_id = 39;

DELETE FROM area_task_subtasks WHERE parent_task_id = 39;

DELETE FROM area_task_assignees WHERE task_id = 39;
DELETE FROM area_task_tags WHERE task_id = 39;

DELETE FROM area_tasks WHERE id = 39;
