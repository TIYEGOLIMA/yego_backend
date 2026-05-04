-- Vacía solo datos del módulo WorkOS / Gantt (tablas JPA en package yego_gantt).
-- PostgreSQL. Haz copia de seguridad antes. Ejemplo:
--   psql "$DATABASE_URL" -f scripts/clear_workos_gantt_data.sql

BEGIN;

DELETE FROM area_task_attachments;
DELETE FROM area_task_subtasks;
DELETE FROM area_task_assignees;
DELETE FROM area_task_tags;
DELETE FROM area_tasks;

DELETE FROM workos_sprints;
DELETE FROM gantt_project_members;
DELETE FROM gantt_projects;

COMMIT;
