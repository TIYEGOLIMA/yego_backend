-- Sincroniza privaciedad con columna authoritativa `private_task` y retira etiquetas reservadas
-- (histórico: marcar privada con tag «privada» / «private»). Ejecutar una vez antes o junto al
-- despliegue que ya no interpreta privacidad desde tags.

BEGIN;

UPDATE area_tasks t
SET private_task = TRUE,
    updated_at   = NOW()
WHERE COALESCE(t.private_task, FALSE) = FALSE
  AND EXISTS (
    SELECT 1
    FROM area_task_tags att
    WHERE att.task_id = t.id
      AND (
        lower(trim(att.tag)) IN ('privada', 'privado', 'private')
            OR lower(trim(att.tag)) LIKE 'privada:%'
            OR lower(trim(att.tag)) LIKE 'private:%'
      )
  );

DELETE FROM area_task_tags att
WHERE lower(trim(att.tag)) IN ('privada', 'privado', 'private')
   OR lower(trim(att.tag)) LIKE 'privada:%'
   OR lower(trim(att.tag)) LIKE 'private:%';

COMMIT;
