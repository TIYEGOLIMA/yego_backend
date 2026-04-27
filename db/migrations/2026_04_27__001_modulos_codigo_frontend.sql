-- Código estable para enlazar cada fila de módulo con una pantalla del frontend.
-- La URL (yeg_sis_ext_url) puede cambiar sin tocar código: el codigo permanece.

ALTER TABLE queue_modulos
    ADD COLUMN IF NOT EXISTS yeg_sis_ext_codigo VARCHAR(64);

-- Poblar según la ruta actual (solo filas sin codigo). Ajusta si tus URLs difieren.
UPDATE queue_modulos
SET yeg_sis_ext_codigo = CASE lower(trim(both '/' from coalesce(yeg_sis_ext_url, '')))
    WHEN 'dashboard' THEN 'DASHBOARD'
    WHEN 'users' THEN 'USERS'
    WHEN 'roles' THEN 'ROLES'
    WHEN 'permissions' THEN 'PERMISSIONS'
    WHEN 'modules' THEN 'MODULES'
    WHEN 'areas' THEN 'AREAS'
    WHEN 'audit' THEN 'AUDIT'
    WHEN 'api-logs' THEN 'API_LOGS'
    WHEN 'sessions' THEN 'SESSIONS'
    WHEN 'reports' THEN 'REPORTS'
    WHEN 'garantizado' THEN 'GARANTIZADO'
    WHEN 'asistencia' THEN 'ASISTENCIA'
    WHEN 'yego-premium' THEN 'YEGO_PREMIUM'
    WHEN 'yego-pro-ops' THEN 'YEGO_PRO_OPS'
    WHEN 'yego-gantt' THEN 'YEGO_GANTT'
    WHEN 'workos' THEN 'YEGO_GANTT'
    WHEN 'mensajes-marketing' THEN 'MENSAJES_MARKETING'
    WHEN 'control-tower' THEN 'CONTROL_TOWER'
    ELSE yeg_sis_ext_codigo
END
WHERE yeg_sis_ext_codigo IS NULL
  AND trim(both '/' from coalesce(yeg_sis_ext_url, '')) <> '';
