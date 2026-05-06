-- Alinear area_task_subtasks con AreaTaskSubtask.createdByUserId (JPA).
-- Ejecutar manualmente en PostgreSQL si ves: column ... created_by_user_id does not exist
-- (spring.jpa.hibernate.ddl-auto=none en application.properties).

ALTER TABLE area_task_subtasks
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT;

COMMENT ON COLUMN area_task_subtasks.created_by_user_id IS 'Usuario que creó la subtarea; filas antiguas pueden quedar NULL';

-- Opcional: FK coherente con users.id (Long). Descomentar si en vuestro entorno ya usáis FKs a users.
-- ALTER TABLE area_task_subtasks
--     ADD CONSTRAINT fk_area_task_subtasks_created_by_user
--     FOREIGN KEY (created_by_user_id) REFERENCES users (id)
--     ON DELETE SET NULL;
