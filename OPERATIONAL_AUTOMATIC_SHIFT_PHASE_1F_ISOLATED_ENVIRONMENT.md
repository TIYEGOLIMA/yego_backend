# Operational Automatic Shift Phase 1F Isolated Environment

## 1. Objective

Preparar un entorno aislado y verificable para repetir Fase 1E con seguridad, sin ejecutar importación Yango real, sin reprocesar turnos reales y sin tocar producción.

Esta fase elimina el blocker principal de 1E:

- el perfil `dev` actual apunta a una base remota compartida;
- no existía perfil local inequívoco para validación operacional;
- no había una ruta segura y automatizable para aplicar/verificar `017` solo sobre localhost;
- no había un smoke check local explícito para validar tablas y endpoints read-only antes de repetir 1E.

## 2. Why Phase 1E Stopped

Fase 1E terminó en `ENVIRONMENT_NOT_SAFE` porque:

- `pom.xml` activa `dev` por defecto;
- `application.properties` apunta a una base PostgreSQL remota compartida;
- `application-dev.yml` también apunta a una base remota compartida;
- `application-prod.yml` usa el mismo host/base aparente;
- el runtime observado no ofrecía evidencia suficiente de aislamiento `local/dev/staging`.

Por eso, en 1E:

- no se aplicó `017`;
- no se ejecutó el runner;
- no se llamo a Yango;
- no se generó evidencia operacional real.

## 3. Current Profiles And Risk

| Perfil | Datasource tipo | Base aparente | Seguro para writes `operational_*` | Observación |
|--------|------------------|---------------|------------------------------------|-------------|
| `default` (`application.properties`) | PostgreSQL remoto | `yego_integral` | NO | Configuración base compartida; no usar para 1E |
| `dev` | PostgreSQL remoto | `yego_integral` | NO | Perfil activo por defecto en Maven; sigue apuntando a remoto |
| `prod` | PostgreSQL remoto | `yego_integral` | NO | Entorno protegido; fuera de alcance para 1F |
| `operational-local` | PostgreSQL local (`localhost`) | `yego_operational_local` | SÍ, si es clon aislado | Perfil nuevo de 1F, runner cerrado por defecto |

Hallazgos adicionales:

- no hay Flyway;
- no hay Liquibase;
- `017_operational_automatic_shift_mirror.sql` existe como SQL manual;
- la ruta segura para 1E pasa por una base local o staging aislada, nunca por `yego_integral`.

## 4. New Local Profile

Archivo nuevo:

- `src/main/resources/application-operational-local.yml`

Propósito:

- obligar a que la validación operacional use `localhost`;
- usar nombre de base distinto de `yego_integral`;
- mantener el runner apagado por defecto;
- fijar `operational.monitoring.runner.environment=local`;
- impedir writes accidentales hasta que exista confirmación explícita.

Defaults seguros:

- `OPERATIONAL_LOCAL_DB_HOST=localhost`
- `OPERATIONAL_LOCAL_DB_PORT=54329`
- `OPERATIONAL_LOCAL_DB_NAME=yego_operational_local`
- `OPERATIONAL_LOCAL_DB_USER=yego_local`
- `OPERATIONAL_LOCAL_DB_PASSWORD=yego_local`
- `OPERATIONAL_LOCAL_SERVER_PORT=3030`

Notas:

- este perfil no modifica `application-dev.yml`;
- este perfil no modifica `application-prod.yml`;
- este perfil no activa importación ni reproceso por defecto;
- este perfil no resuelve por sí solo el schema completo de la app: debe apuntar a un clon aislado/restaurado de la base aplicativa antes del smoke check HTTP completo.

## 5. Docker / Local DB

Archivo nuevo:

- `docker-compose.operational-local.yml`

Qué levanta:

- un PostgreSQL local aislado para pruebas operacionales;
- puerto local configurable con default `54329`;
- base default `yego_operational_local`;
- usuario local sin secretos reales;
- volumen local `yego_operational_local_pgdata`.

Comandos de ejemplo:

```powershell
docker compose -f docker-compose.operational-local.yml up -d
docker compose -f docker-compose.operational-local.yml ps
docker compose -f docker-compose.operational-local.yml down
```

Advertencias:

- el contenedor solo crea una base vacía;
- para que el app completo y los endpoints de validación funcionen como en Fase 1E, primero debe restaurarse un clon aislado del schema/datos base de la app;
- luego recién debe aplicarse `017`.

## 6. How To Apply Or Verify Migration 017

Script nuevo:

- `scripts/apply-operational-local-migration.ps1`

Seguridad implementada:

- exige `CONFIRM_OPERATIONAL_LOCAL_DB=true`;
- falla si el host no es `localhost` o `127.0.0.1`;
- falla si la DB objetivo es `yego_integral`;
- no modifica `017`;
- no crea migraciones nuevas;
- aplica solo `src/main/resources/db/migration/017_operational_automatic_shift_mirror.sql`;
- verifica existencia de:
  - `operational_trip_facts`
  - `operational_shift_sessions`
  - `operational_shift_events`

Prerequisitos:

- cliente `psql` instalado;
- base local levantada;
- schema base ya restaurado si se quiere validar también endpoints de manual comparison / summary.

Ejemplo seguro:

```powershell
$env:CONFIRM_OPERATIONAL_LOCAL_DB = "true"
$env:OPERATIONAL_LOCAL_DB_HOST = "localhost"
$env:OPERATIONAL_LOCAL_DB_PORT = "54329"
$env:OPERATIONAL_LOCAL_DB_NAME = "yego_operational_local"
$env:OPERATIONAL_LOCAL_DB_USER = "yego_local"
$env:OPERATIONAL_LOCAL_DB_PASSWORD = "yego_local"

.\scripts\apply-operational-local-migration.ps1
```

## 7. How To Smoke Check Without Yango

Script nuevo:

- `scripts/smoke-check-operational-local.ps1`

Verifica:

1. target local explícito;
2. `CONFIRM_OPERATIONAL_LOCAL_DB=true`;
3. runner desactivado por defecto en `application-operational-local.yml`;
4. existencia de tablas:
   - `shift_sessions`
   - `module_driver_closes`
   - `operational_trip_facts`
   - `operational_shift_sessions`
   - `operational_shift_events`
5. opcionalmente, endpoints read-only HTTP con `-CheckHttp`.

Importante:

- no llama Yango;
- no ejecuta runner;
- no escribe en tablas manuales;
- las verificaciones HTTP requieren que la app ya esté levantada con perfil `operational-local`;
- las verificaciones HTTP completas requieren un clon local/restaurado del schema base, no solo una base vacía.

Ejemplo de DB-only smoke check:

```powershell
$env:CONFIRM_OPERATIONAL_LOCAL_DB = "true"
.\scripts\smoke-check-operational-local.ps1
```

Ejemplo con HTTP:

```powershell
$env:CONFIRM_OPERATIONAL_LOCAL_DB = "true"
.\scripts\smoke-check-operational-local.ps1 -CheckHttp -From 2026-06-23 -To 2026-06-23
```

## 8. How To Prepare The Real Phase 1E Run

No ejecutar 1E sobre:

- `application-dev.yml` actual;
- `application.properties` actual;
- host remoto compartido;
- base `yego_integral`;
- environment `unknown`.

Preparación recomendada:

1. levantar PostgreSQL local aislado o usar staging realmente aislado;
2. restaurar clon seguro del schema base de la app;
3. aplicar `017` con el script local;
4. correr smoke check sin Yango;
5. levantar app con perfil `operational-local`;
6. solo entonces preparar la corrida 1E real.

## 9. Mandatory Runner Properties

Para una futura repetición segura de 1E:

- `operational.monitoring.runner.enabled=true`
- `operational.monitoring.runner.environment=local`
- `operational.monitoring.runner.confirm-writes-to-operational-tables=true`
- `operational.monitoring.runner.date-from=YYYY-MM-DD`
- `operational.monitoring.runner.date-to=YYYY-MM-DD`

Opcionales:

- `operational.monitoring.runner.driver-ids[0]=driver-a`
- `operational.monitoring.runner.driver-ids[1]=driver-b`
- `operational.monitoring.runner.vehicle-key=...`

Límites:

- rango máximo: 1 día;
- conductores: máximo 5 recomendado para 1E, hard cap 20 en guard;
- nunca ejecutar si `environment=unknown|prod|production`.

## 10. Example Commands Without Secrets

Levantar DB local:

```powershell
docker compose -f docker-compose.operational-local.yml up -d
```

Aplicar `017` en local:

```powershell
$env:CONFIRM_OPERATIONAL_LOCAL_DB = "true"
.\scripts\apply-operational-local-migration.ps1
```

Levantar app con perfil local aislado:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=operational-local"
```

Smoke check DB + HTTP:

```powershell
$env:CONFIRM_OPERATIONAL_LOCAL_DB = "true"
.\scripts\smoke-check-operational-local.ps1 -CheckHttp -From 2026-06-23 -To 2026-06-23
```

Preparar futura corrida 1E local, sin ejecutarla en esta fase:

```powershell
mvn spring-boot:run `
  "-Dspring-boot.run.profiles=operational-local" `
  "-Dspring-boot.run.arguments=--operational.monitoring.runner.enabled=true,--operational.monitoring.runner.environment=local,--operational.monitoring.runner.confirm-writes-to-operational-tables=true,--operational.monitoring.runner.date-from=2026-06-23,--operational.monitoring.runner.date-to=2026-06-23,--operational.monitoring.runner.driver-ids[0]=driver-a,--operational.monitoring.runner.driver-ids[1]=driver-b"
```

Consultar endpoints read-only:

```powershell
Invoke-WebRequest "http://localhost:3030/api/pro-ops/operational-monitoring/trip-facts?from=2026-06-23&to=2026-06-23"
Invoke-WebRequest "http://localhost:3030/api/pro-ops/operational-monitoring/shifts?from=2026-06-23&to=2026-06-23"
Invoke-WebRequest "http://localhost:3030/api/pro-ops/operational-monitoring/events?from=2026-06-23&to=2026-06-23"
Invoke-WebRequest "http://localhost:3030/api/pro-ops/operational-monitoring/validation/coverage?from=2026-06-23&to=2026-06-23"
Invoke-WebRequest "http://localhost:3030/api/pro-ops/operational-monitoring/validation/summary?from=2026-06-23&to=2026-06-23"
```

## 11. Rollback

Si se decide retirar 1F:

1. revertir el commit de esta fase;
2. detener y eliminar el contenedor local:
   - `docker compose -f docker-compose.operational-local.yml down -v`
3. borrar la base local o volumen si corresponde;
4. eliminar variables `OPERATIONAL_LOCAL_*` de la sesión;
5. confirmar que el app vuelve a depender solo de los perfiles anteriores.

## 12. No-Touch Verification

No se modificaron:

- `ShiftSession.java`
- `Trip.java`
- `DriverClose.java`
- `FacturacionSemanal.java`
- `PaymentPercentage.java`
- `BonusThreshold.java`
- `LiquidacionServiceImpl.java`
- `DriverCloseServiceImpl.java`
- `FacturacionSemanalServiceImpl.java`
- `LiquidacionController.java`
- `ShiftSessionController.java`
- frontend
- clientes/servicios Yango existentes
- migraciones existentes

## 13. Checklist Before Executing Import

Antes de repetir 1E real:

- usar perfil `operational-local` o staging realmente aislado;
- confirmar host `localhost` o target aislado equivalente;
- confirmar DB distinta de `yego_integral`;
- confirmar `017` aplicada;
- confirmar tablas `operational_*` existentes;
- confirmar smoke check DB y HTTP;
- confirmar runner apagado por defecto;
- activar runner solo con properties explícitas;
- confirmar rango máximo de 24 horas;
- confirmar muestra máxima de 5 conductores;
- confirmar que la corrida sigue sin tocar liquidación ni tablas manuales.

## 14. Recommended Next Phase

Siguiente fase recomendada:

- repetir Fase 1E sobre `operational-local` o staging realmente aislado;
- seleccionar ventana operativa de 24 horas;
- importar muestra limitada;
- reprocesar solo el rango controlado;
- medir `coverage`, `summary`, `mismatches` y `manual-comparison`;
- seguir conservadoramente en modo paralelo.
