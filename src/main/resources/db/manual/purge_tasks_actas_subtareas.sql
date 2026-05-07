-- Purga: solo datos de tareas Gantt, subtareas, mensajes de chat de tareas y actas (minutas).
-- NO toca: usuarios, áreas, workspaces (gantt_projects), sprints, miembros de proyecto, etc.

BEGIN;

DELETE FROM workos_task_messages;

DELETE FROM area_task_subtasks;

DELETE FROM area_task_assignees;

DELETE FROM area_task_tags;

DELETE FROM area_tasks;

DELETE FROM workos_meeting_minute_items;

DELETE FROM workos_meeting_minutes;

COMMIT;

-- Si alguna tabla del listado no existe en tu esquema, comenta esa línea y vuelve a ejecutar.
