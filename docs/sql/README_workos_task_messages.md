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

## Dónde aparece el chat en la aplicación (Fase 1)

- El chat de tarea vive en el **modal de detalle** al abrir una tarea desde **Board**, **Portfolio** y los flujos que invocan **`onOpenTaskById`** (por ejemplo Actas o Calendario).
- La vista **Timeline** usa otro panel lateral y **no** incluye este chat en Fase 1; no es una incoherencia accidental sino el alcance acordado hasta integrar otra entrada de UX si se decide en Fase 2+.

Plantilla de evidencia, tabla de red/BD y decisión **GO / NO-GO**: **[docs/workos-chat-fase1-go-no-go.md](../../../docs/workos-chat-fase1-go-no-go.md)** (desde la raíz del repo: `docs/workos-chat-fase1-go-no-go.md`).

### Red esperada al abrir el detalle

Con el hilo “Toda la tarea”: **una** petición de **subtareas** y **una** de **mensajes**. Al cambiar de subtarea en el selector del chat, se dispara **otra** petición de mensajes (esperado). En desarrollo, `React.StrictMode` puede duplicar efectos: puede verse una request cancelada y otra 200; para medir tiempos es más fiable un build de producción o tomar la request final no cancelada (ver documento enlazado).

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
