-- Reinicia datos operativos de WorkOS / Yego Gantt (proyectos, miembros, tareas, sprints).
-- No toca users, areas, roles ni otros módulos.
--
-- Uso (PostgreSQL), desde la raíz del backend:
--   psql "$DATABASE_URL" -f sql/workos_reset_data.sql
-- o conexión explícita a tu instancia.

BEGIN;

DELETE FROM area_task_assignees;
DELETE FROM area_task_tags;
DELETE FROM area_tasks;
DELETE FROM workos_sprints;
DELETE FROM gantt_project_members;
DELETE FROM gantt_projects;

COMMIT;

-- Opcional: que los próximos IDs empiecen desde 1 (nombres típicos de secuencias en PostgreSQL).
-- Descomenta si aplica a tu esquema:
-- ALTER SEQUENCE IF EXISTS area_tasks_id_seq RESTART WITH 1;
-- ALTER SEQUENCE IF EXISTS workos_sprints_id_seq RESTART WITH 1;
-- ALTER SEQUENCE IF EXISTS gantt_projects_id_seq RESTART WITH 1;
-- ALTER SEQUENCE IF EXISTS gantt_project_members_id_seq RESTART WITH 1;
