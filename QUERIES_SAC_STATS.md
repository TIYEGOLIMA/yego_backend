# Queries SQL para el Endpoint `/api/ticketera/sac-stats`

## Parámetros
- **fechaInicio**: `2025-12-08`
- **fechaFin**: `2025-12-14`
- **Zona horaria**: `America/Lima` (UTC-5)

## Conversión de Fechas
- **fechaInicio**: `2025-12-08 00:00:00` (inicio del día en America/Lima)
- **fechaFin**: `2025-12-14 23:59:59.999999999` (fin del día en America/Lima)

---

## Query Principal: Obtener Tickets en el Rango de Fechas

Esta es la query principal que obtiene todos los tickets de usuarios SAC en el rango de fechas:

```sql
SELECT t.* 
FROM tickets t
WHERE t.user_id IN (
    SELECT u.id 
    FROM users u
    JOIN roles r ON r.id = u.role
    WHERE r.name = 'SAC'
)
AND t.created_at >= '2025-12-08 00:00:00'
AND t.created_at <= '2025-12-14 23:59:59.999999'
ORDER BY t.created_at ASC;
```

### Versión Simplificada (si ya conoces los IDs de usuarios SAC):

```sql
SELECT * 
FROM tickets 
WHERE user_id IN (1, 2, 3, 4, 5)  -- Reemplaza con los IDs reales de usuarios SAC
AND created_at >= '2025-12-08 00:00:00'
AND created_at <= '2025-12-14 23:59:59.999999'
ORDER BY created_at ASC;
```

---

## Todas las Queries que se Ejecutan

### 1. Obtener Usuarios SAC

```sql
SELECT u.*, r.*
FROM users u
JOIN roles r ON r.id = u.role
WHERE r.name = 'SAC';
```

### 2. Obtener Tickets de Usuarios SAC en el Rango de Fechas

```sql
SELECT t.*
FROM tickets t
WHERE t.user_id IN (1, 2, 3, 4, 5)  -- IDs de usuarios SAC
AND t.created_at >= '2025-12-08 00:00:00'
AND t.created_at <= '2025-12-14 23:59:59.999999';
```

### 3. Contar Ratings en el Rango de Fechas

```sql
SELECT COUNT(*)
FROM queue_ratings
WHERE created_at >= '2025-12-08 00:00:00'
AND created_at <= '2025-12-14 23:59:59.999999';
```

### 4. Obtener Promedio de Ratings en el Rango de Fechas

```sql
SELECT AVG(score)
FROM queue_ratings
WHERE created_at >= '2025-12-08 00:00:00'
AND created_at <= '2025-12-14 23:59:59.999999';
```

### 5. Obtener Ratings de Tickets Completados en el Rango de Fechas

```sql
SELECT qr.*
FROM queue_ratings qr
WHERE qr.ticket_id IN (
    SELECT t.id 
    FROM tickets t
    WHERE t.user_id IN (1, 2, 3, 4, 5)  -- IDs de usuarios SAC
    AND t.status = 'COMPLETED'
    AND t.created_at >= '2025-12-08 00:00:00'
    AND t.created_at <= '2025-12-14 23:59:59.999999'
)
AND qr.created_at >= '2025-12-08 00:00:00'
AND qr.created_at <= '2025-12-14 23:59:59.999999';
```

### 6. Obtener Ratings Recientes (Top 10) en el Rango de Fechas

```sql
SELECT qr.*
FROM queue_ratings qr
WHERE qr.created_at >= '2025-12-08 00:00:00'
AND qr.created_at <= '2025-12-14 23:59:59.999999'
ORDER BY qr.created_at DESC
LIMIT 10;
```

### 7. Obtener Tickets Completados para Cálculo de Tiempo de Respuesta

```sql
SELECT t.*
FROM tickets t
WHERE t.user_id IN (1, 2, 3, 4, 5)  -- IDs de usuarios SAC
AND t.status = 'COMPLETED'
AND t.called_at IS NOT NULL
AND t.completed_at IS NOT NULL
AND t.created_at >= '2025-12-08 00:00:00'
AND t.created_at <= '2025-12-14 23:59:59.999999';
```

---

## Query Completa para Análisis Directo en PostgreSQL

Si quieres ejecutar una sola query que te dé todos los tickets con información completa:

```sql
SELECT 
    t.id,
    t.ticket_number,
    t.user_id,
    u.name AS usuario_nombre,
    u.username AS usuario_username,
    r.name AS rol_nombre,
    t.status,
    t.priority,
    t.created_at,
    t.called_at,
    t.completed_at,
    t.license_number,
    t.agent_id,
    t.module_id
FROM tickets t
JOIN users u ON u.id = t.user_id
JOIN roles r ON r.id = u.role
WHERE r.name = 'SAC'
AND t.created_at >= '2025-12-08 00:00:00'
AND t.created_at <= '2025-12-14 23:59:59.999999'
ORDER BY t.created_at ASC;
```

---

## Notas Importantes

1. **Zona Horaria**: Las fechas se convierten a `America/Lima` (UTC-5). Si tu base de datos almacena en UTC, necesitarás ajustar las fechas:
   - `2025-12-08 00:00:00` en America/Lima = `2025-12-08 05:00:00` UTC
   - `2025-12-14 23:59:59` en America/Lima = `2025-12-15 04:59:59` UTC

2. **Formato de Fecha**: PostgreSQL acepta el formato `YYYY-MM-DD HH:MM:SS` directamente.

3. **Rol SAC**: El código busca usuarios con rol `'SAC'`, `'sac'` o `'Sac'` (case-insensitive en la lógica, pero la query es case-sensitive).

4. **Status de Tickets**: Los tickets pueden tener los siguientes estados:
   - `WAITING`
   - `CALLED`
   - `IN_PROGRESS`
   - `COMPLETED`
   - `CANCELLED`

