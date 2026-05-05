# Migración: `workos_task_messages` (WorkOS chat Fase 1)

## Script

Aplicar en **PostgreSQL** el archivo **[V001__workos_task_messages.sql](V001__workos_task_messages.sql)** en cada entorno **antes** de usar el chat en tareas.

```bash
psql "$DATABASE_URL" -f docs/sql/V001__workos_task_messages.sql
```

La migración es **aditiva**: crea tabla, FKs (`area_tasks`, `area_task_subtasks`, `users`), CHECK de `message_type` e índices. No altera destructivamente tablas existentes.

**Rendimiento (opcional):** si el GET de mensajes es lento con muchas filas, aplicar también **[V002__workos_task_messages_performance_indexes.sql](V002__workos_task_messages_performance_indexes.sql)** (índices parciales para `is_deleted = false`). Tras cargar datos, conviene **`ANALYZE workos_task_messages`** (y `users` si el JOIN va lento).

## Flyway / Liquibase

En este repositorio **no** está configurado Flyway en `pom.xml`. Si el equipo lo adopta, copiar el contenido de `V001__workos_task_messages.sql` como script versionado correspondiente (`Vxxx__workos_task_messages.sql` bajo `src/main/resources/db/migration/`).

## API REST

Base de mensajes: **`/api/yego-gantt/tasks/{taskId}/messages`** (JWT como el resto de `/api`).

| Método | Ruta |
|--------|------|
| GET | `/api/yego-gantt/tasks/{taskId}/messages` |
| POST | `/api/yego-gantt/tasks/{taskId}/messages` |
| DELETE | `/api/yego-gantt/tasks/{taskId}/messages/{messageId}` |
| PUT | `/api/yego-gantt/tasks/{taskId}/messages/{messageId}` |

Query opcional en GET: `subtaskId` (hilo de una subtarea).

El frontend Integral llama **`/yego-gantt/tasks/...`** en el cliente HTTP (el servicio antepone **`/api`**).

## Checklist QA manual (GO/NO-GO)

Marque tras probar en un entorno con la migración aplicada.

**Backend**

- [ ] POST mensaje USER con contenido válido en tarea existente (autenticado).
- [ ] GET lista ordenada; mensajes con `is_deleted` no aparecen.
- [ ] POST vacío o >5000 caracteres → error de validación.
- [ ] POST en tarea inexistente o sin permiso → 404/403.
- [ ] DELETE soft: autor, ADMIN/SUPERADMIN o quien puede mutar la tarea según `AreaTaskAccessHelper`.
- [ ] PUT edita solo mensaje USER propio; no edita SYSTEM.
- [ ] Tras `PUT` tarea, cambios de estado/prioridad/fechas/progreso/asignados generan mensajes SYSTEM; sin cambios reales no se generan líneas duplicadas.

**Frontend**

- [ ] Detalle de tarea → panel “Conversación”; con subtareas, selector “Toda la tarea” / subtarea.
- [ ] Enviar mensaje y recargar página: persiste.
- [ ] Empty state tarea / subtarea.
- [ ] Error de red mostrado sin bloquear el resto del modal.

**Compilación**

- [ ] `mvn -q compile -DskipTests` (backend)
- [ ] `npm run build` (frontend, raíz `yego_frontend`)

**GO** si lo anterior pasa y no se rompen Timeline/Board/login. **NO-GO** si falla edición de tareas, carga WorkOS o la migración no está aplicada en la BD usada por Spring.

## Fase 2 sugerida

- Mensajes `RESOLUTION` ligados a cierre formal de tarea.
- WebSockets opcionales.
- Edición en línea / threads.
