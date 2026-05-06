-- =============================================================================
-- Limpieza WorkOS / Gantt: espacios de trabajo, sprints, actas y tareas de equipo
-- CONSERVA “Mi espacio” en el sentido de dominio actual:
--   - Tareas SIN espacio (project_id IS NULL)
--   - Tareas PRIVADAS (private_task = TRUE), aunque estuvieran en un proyecto
--
-- NO ejecutar en producción sin backup y sin revisar el WHERE de borrado de tareas.
--
-- Uso (ejemplo):
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f docs/sql/cleanup_workspaces_keep_mi_espacio.sql
-- =============================================================================

BEGIN;

-- 1) Actas (ítems primero por FK lógica JPA)
DELETE FROM workos_meeting_minute_items;
DELETE FROM workos_meeting_minutes;

-- 2) Identificar tareas “de espacio de trabajo / equipo” a borrar:
--    están en un proyecto Y no son privadas personales.
--    Colecciones JPA (asignados, tags) y subtareas: borrar antes si la BD no tiene ON DELETE CASCADE.

DELETE FROM area_task_assignees WHERE task_id IN (
    SELECT t.id FROM area_tasks t
    WHERE t.project_id IS NOT NULL
      AND COALESCE(t.private_task, FALSE) IS NOT TRUE
);
DELETE FROM area_task_tags WHERE task_id IN (
    SELECT t.id FROM area_tasks t
    WHERE t.project_id IS NOT NULL
      AND COALESCE(t.private_task, FALSE) IS NOT TRUE
);

DELETE FROM area_task_subtasks AS st
WHERE st.parent_task_id IN (
    SELECT t.id FROM area_tasks t
    WHERE t.project_id IS NOT NULL
      AND COALESCE(t.private_task, FALSE) IS NOT TRUE
);

DELETE FROM area_tasks
WHERE project_id IS NOT NULL
  AND COALESCE(private_task, FALSE) IS NOT TRUE;

-- 3) Tareas conservadas (.sin espacio. o privadas): quitar vínculos a proyecto/sprint
--    antes de borrar proyectos y sprints.
UPDATE area_tasks SET sprint_id = NULL WHERE sprint_id IS NOT NULL;

UPDATE area_tasks
SET project_id = NULL
WHERE project_id IS NOT NULL;

-- 4) Eliminar todos los sprints (pertenecían a proyectos WorkOS).
DELETE FROM workos_sprints;

-- 5) Miembros y espacios de trabajo (tabla Project / gantt_projects).
DELETE FROM gantt_project_members;
DELETE FROM gantt_projects;

COMMIT;

-- =============================================================================
-- Tras ejecutar:
-- - Revisa en la UI Board / Portfolio que solo queden tareas esperadas en “Mi espacio”.
-- - Si usas caché de cliente, recarga dura.
-- =============================================================================
