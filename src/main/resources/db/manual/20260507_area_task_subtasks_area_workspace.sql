-- Área y espacio de trabajo por subtarea (herencia desde el padre si quedan NULL).
-- DDL manual si spring.jpa.hibernate.ddl-auto=none.

ALTER TABLE area_task_subtasks
    ADD COLUMN IF NOT EXISTS area_id BIGINT;

ALTER TABLE area_task_subtasks
    ADD COLUMN IF NOT EXISTS project_id BIGINT;

-- Heredar del padre en filas existentes sin valor
UPDATE area_task_subtasks s
SET area_id = t.area_id,
    project_id = t.project_id
FROM area_tasks t
WHERE t.id = s.parent_task_id
  AND (s.area_id IS NULL OR s.project_id IS NULL);

ALTER TABLE area_task_subtasks
    ADD COLUMN IF NOT EXISTS description VARCHAR(4000);

