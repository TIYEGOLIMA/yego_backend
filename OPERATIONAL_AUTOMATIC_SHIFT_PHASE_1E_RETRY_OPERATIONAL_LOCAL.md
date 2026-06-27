# Operational Automatic Shift Phase 1E Retry Operational Local

## 1. Executive decision

LOCAL_ENVIRONMENT_NOT_READY

La repetición de Fase 1E no se ejecutó porque el entorno `operational-local` no estaba operativo en esta máquina al momento de la verificación. Aunque la configuración aislada de Fase 1F existe en la repo, no se pudo confirmar una base local accesible en el target esperado ni una ruta utilizable para aplicar/verificar `017` y validar baseline manual.

Bloqueadores observados:

- el perfil `operational-local` existe y está correctamente aislado en código;
- el host esperado sigue siendo `localhost`;
- la base esperada sigue siendo `yego_operational_local`;
- el puerto esperado `54329` no respondió;
- Docker Desktop no estuvo disponible para levantar `docker-compose.operational-local.yml`;
- `psql` no estuvo disponible en `PATH`;
- no había variables `OPERATIONAL_LOCAL_*` activas en la sesión;
- por lo tanto no se pudo verificar baseline manual local, ni migración `017`, ni smoke check DB-only real.

Por seguridad:

- no se ejecutó importación Yango;
- no se ejecutó reproceso;
- no se activó el runner;
- no se hicieron writes en `operational_*`;
- no se tocaron tablas manuales ni financieras.

## 2. Pre-check

- Branch actual: `master`
- `git status`: limpio al inicio de la fase
- HEAD actual: `13b72a7`
- Commit esperado confirmado:
  - `13b72a7` chore(pro-ops): add isolated operational validation environment
- Lecturas confirmadas:
  - `OPERATIONAL_AUTOMATIC_SHIFT_PHASE_1F_ISOLATED_ENVIRONMENT.md`
  - `OPERATIONAL_AUTOMATIC_SHIFT_PHASE_1E_ONE_DAY_REAL_RUN.md`
  - `OPERATIONAL_AUTOMATIC_SHIFT_PHASE_1D_SAFE_RUNNER.md`
  - `OPERATIONAL_AUTOMATIC_SHIFT_PHASE_1B_VALIDATION.md`
- Perfil objetivo confirmado:
  - `operational-local`
- Perfiles descartados para esta fase:
  - `dev`
  - `prod`
  - `default` remoto
  - cualquier target `yego_integral`
- Properties del runner confirmadas:
  - `operational.monitoring.runner.enabled`
  - `operational.monitoring.runner.environment`
  - `operational.monitoring.runner.confirm-writes-to-operational-tables`
  - `operational.monitoring.runner.max-range-days`
  - `operational.monitoring.runner.default-driver-count`
  - `operational.monitoring.runner.max-driver-count`
  - `operational.monitoring.runner.use-manual-sample-selector`
  - `operational.monitoring.runner.date-from`
  - `operational.monitoring.runner.date-to`
  - `operational.monitoring.runner.driver-ids`
  - `operational.monitoring.runner.vehicle-key`
- No se leyeron ni imprimieron secretos.
- No se modificó código en esta fase.

## 3. Local environment safety

Estado del target esperado por perfil:

- host esperado: `localhost`
- DB esperada: `yego_operational_local`
- puerto esperado: `54329`
- runner por defecto: `disabled`
- `operational.monitoring.runner.environment`: `local`

Verificaciones ejecutadas:

- `application-operational-local.yml` confirma target local aislado;
- `docker compose -f docker-compose.operational-local.yml ps` falló porque Docker Desktop no estuvo disponible;
- `docker compose -f docker-compose.operational-local.yml up -d` también falló por la misma razón;
- `Test-NetConnection localhost:54329` devolvió `TcpTestSucceeded=False`;
- no había variables `OPERATIONAL_LOCAL_*` activas;
- sí existe un servicio PostgreSQL local en Windows (`postgresql-x64-18`), pero no quedó demostrado que corresponda al target `yego_operational_local` ni que escuche en `54329`.

Decisión:

- el target local requerido por 1F/1E Retry no está listo para usarse de forma verificable;
- no continuar a baseline, migración ni runner.

Resultado:

- `LOCAL_ENVIRONMENT_NOT_READY`

## 4. Local manual baseline

No verificado.

Motivo:

- no se pudo establecer conexión utilizable al target `localhost:54329/yego_operational_local`;
- no estuvo disponible `psql`;
- no hubo contenedor Docker local activo para el target esperado.

Por lo tanto no se pudo confirmar:

- existencia de `shift_sessions`;
- existencia de `module_driver_closes`;
- conteos de baseline manual;
- rango de fechas manuales;
- disponibilidad local de placas en `DriverClose`.

## 5. Migration 017 result

No aplicada y no verificada sobre DB local.

Estado:

- el archivo `src/main/resources/db/migration/017_operational_automatic_shift_mirror.sql` existe en la repo;
- el script `scripts/apply-operational-local-migration.ps1` existe;
- el script depende de un target local real y de `psql`;
- `psql` no estuvo disponible en esta máquina;
- el target `localhost:54329/yego_operational_local` no estuvo accesible.

Resultado:

- `MIGRATION_NOT_EXECUTED_BECAUSE_LOCAL_TARGET_NOT_READY`

## 6. Smoke check result

No ejecutado completamente.

Estado:

- `scripts/smoke-check-operational-local.ps1` existe;
- el smoke check DB-only no podía completarse sin conexión válida al target local y sin `psql`;
- el smoke check HTTP no correspondía ejecutarlo porque primero falló el gate DB-only;
- no se levantó la app con `operational-local` para endpoints read-only.

Resultado:

- `SMOKE_CHECK_NOT_EXECUTED_FOR_LOCAL_READINESS_FAILURE`

## 7. Operational window selected

No seleccionada.

Motivo:

- al no verificarse baseline manual local, no corresponde escoger ventana operativa;
- no se debe inferir una ventana “a ciegas” sin `shift_sessions` y `DriverClose` locales verificables.

Resultado:

- `operational_window`: none
- `calendar_day`: none
- `cross_midnight_target`: not evaluated

## 8. Drivers selected

No seleccionados.

Motivo:

- no se pudo leer baseline manual local;
- sin baseline local no es seguro ni útil escoger conductores para importación.

Resultado:

- conductores: `0`

## 9. Runner command and properties

Runner no ejecutado.

El comando que habría sido válido, si todos los gates hubieran pasado, seguía siendo uno con:

- perfil `operational-local`
- `operational.monitoring.runner.enabled=true`
- `operational.monitoring.runner.environment=local`
- `operational.monitoring.runner.confirm-writes-to-operational-tables=true`
- `operational.monitoring.runner.date-from=<from>`
- `operational.monitoring.runner.date-to=<to>`
- hasta `5` conductores explícitos

En esta fase no se lanzó ningún comando con el runner activo.

## 10. Import results

No ejecutado.

Resultado:

- `Yango calls executed`: NO
- `import result`: `IMPORT_NOT_EXECUTED_FOR_LOCAL_ENVIRONMENT_NOT_READY`
- `operational_trip_facts`: `0`

## 11. Reprocess results

No ejecutado.

Resultado:

- `reprocess result`: `REPROCESS_NOT_EXECUTED_FOR_LOCAL_ENVIRONMENT_NOT_READY`
- `operational_shift_sessions`: `0`
- `operational_shift_events`: `0`

## 12. Validation coverage

No disponible.

Motivo:

- no hubo importación;
- no hubo reproceso;
- no se consultaron endpoints read-only;
- no se confirmó la capa `operational_*` en el target local.

## 13. Validation summary

No disponible.

Métricas no observadas:

- `manualShiftCount`
- `matchedShiftCount`
- `unmatchedOperationalShiftCount`
- `unmatchedManualShiftCount`
- `needsReviewShiftCount`
- `needsReviewShiftPct`
- `autoClosedByNextDriverCount`
- `staleCandidateCount`
- `averageStartDeltaMinutes`
- `averageEndDeltaMinutes`
- `p95StartDeltaMinutes`
- `p95EndDeltaMinutes`
- `manualReplacementReadiness`

## 14. Manual comparison findings

No hay hallazgos nuevos.

Motivo:

- no se ejecutó la corrida;
- no se produjo data operacional local;
- no se consultó `manual-comparison`.

## 15. Mismatch findings

No hay mismatches nuevos.

Motivo:

- no se generó evidencia operacional local;
- no se consultó `mismatches`.

## 16. Day/night/cross-midnight analysis

No evaluado.

Motivo:

- no hubo ventana operativa real;
- no hubo baseline manual leído localmente;
- no hubo turnos automáticos inferidos.

Conclusión:

- sigue pendiente validar turnos diurnos;
- sigue pendiente validar turnos nocturnos;
- sigue pendiente validar cruce de medianoche;
- sigue pendiente medir si el motor corta incorrectamente por fecha calendario.

## 17. Sensitivity learnings

No hubo evidencia nueva de sensibilidad porque no se ejecutó la corrida.

Sí se mantiene el aprendizaje previo:

- la futura validación debe observar explícitamente día, noche y cruce de medianoche;
- será necesaria una configuración futura de sensibilidad por contexto operativo;
- no corresponde implementarla antes de tener al menos una corrida local verificable.

## 18. Feedback learnings

No hubo mismatches reales nuevos para retroalimentación.

Sigue siendo útil mantener preparado el catálogo conceptual de feedback:

- `AUTO_SHIFT_CORRECT`
- `AUTO_OPENED_TOO_EARLY`
- `AUTO_OPENED_TOO_LATE`
- `AUTO_CLOSED_TOO_EARLY`
- `AUTO_CLOSED_TOO_LATE`
- `WRONG_DRIVER`
- `WRONG_VEHICLE`
- `MISSING_VEHICLE`
- `NIGHT_SHIFT_CROSSED_MIDNIGHT_OK`
- `NIGHT_SHIFT_SPLIT_WRONGLY`
- `MANUAL_SHIFT_WRONG`
- `YANGO_DATA_DELAYED`
- `IGNORE_CASE`

## 19. UI readiness

`NOT_READY`

Motivo:

- no existe todavía evidencia real de una corrida local operacional;
- no hay coverage, summary ni mismatch reales;
- no se verificó comportamiento día/noche/medianoche.

## 20. Manual replacement readiness

`NOT_READY`

Motivo:

- no hubo baseline local verificado;
- no hubo corrida automática real;
- no hubo matching manual vs automático;
- esta fase sigue siendo estrictamente preparatoria/diagnóstica.

## 21. Required fixes before 7-day run

- levantar de verdad el target `localhost:54329` o ajustar explícitamente el puerto local si el equipo decide otro valor;
- restaurar un clon local aislado del schema/datos base de la app;
- confirmar que `shift_sessions` y `module_driver_closes` existen y tienen datos;
- instalar o habilitar `psql`, o proveer una ruta equivalente y segura para aplicar/verificar `017`;
- aplicar `017` sobre `yego_operational_local`;
- ejecutar smoke check DB-only y luego HTTP;
- recién después repetir 1E para 1 día;
- solo si eso funciona, evaluar una corrida de 7 días como 7 corridas separadas de 1 día.

## 22. Required fixes before UI

- completar una corrida 1E Retry real con baseline local disponible;
- obtener `coverage`, `summary`, `mismatches` y `manual-comparison`;
- validar casos diurnos, nocturnos y con cruce de medianoche;
- documentar conductores y ventanas útiles;
- identificar mismatches dominantes y calidad de `vehicle_key`.

## 23. No-touch verification

Confirmado:

- no se modificó código backend;
- no se crearon endpoints nuevos;
- no se creó scheduler;
- no se tocaron migraciones existentes;
- no se tocaron tablas manuales;
- no se tocó liquidación;
- no se tocaron pagos;
- no se tocaron tiers;
- no se tocaron gastos;
- no se tocó frontend;
- no se tocaron clientes/servicios Yango existentes.

## 24. Final recommendation

No intentar la corrida 1E Retry hasta que el entorno local exista de forma verificable.

Secuencia recomendada:

1. asegurar que PostgreSQL local escuche en `54329` o documentar el puerto local real;
2. definir y exportar `OPERATIONAL_LOCAL_*` de forma explícita;
3. restaurar baseline manual local aislado;
4. habilitar `psql` o una herramienta equivalente segura;
5. aplicar `017`;
6. correr smoke check DB-only;
7. correr smoke check HTTP;
8. recién entonces repetir 1E Retry.

VERDICT:

- Executive decision: LOCAL_ENVIRONMENT_NOT_READY
- Branch: master
- Commit: 13b72a7
- Profile used: operational-local
- DB host: localhost
- DB name: yego_operational_local
- Local baseline status: NOT_VERIFIED
- Migration 017 applied/existed: NO
- Operational tables existed after migration: NO
- Runner executed: NO
- Runner guard passed: NO
- Operational window tested: NONE
- Shift types observed: NONE
- Cross-midnight observed: NO
- Drivers tested: 0
- Yango calls executed: NO
- Yango call scope: NONE
- Import result: IMPORT_NOT_EXECUTED_FOR_LOCAL_ENVIRONMENT_NOT_READY
- Reprocess result: REPROCESS_NOT_EXECUTED_FOR_LOCAL_ENVIRONMENT_NOT_READY
- Operational trip facts: 0
- Operational shifts: 0
- Vehicle key coverage: N/A
- Matched shift pct: N/A
- Needs review pct: N/A
- Main mismatch type: N/A
- Day/night issue found: YES
- Sensitivity config needed: YES
- Feedback module needed: YES
- UI readiness: NOT_READY
- Manual replacement readiness: NOT_READY
- Files changed: OPERATIONAL_AUTOMATIC_SHIFT_PHASE_1E_RETRY_OPERATIONAL_LOCAL.md
- Migrations created: NO
- Existing tables altered: NO
- Financial logic touched: NO
- `shift_sessions` touched: NO
- `trips` touched: NO
- `DriverClose` touched: NO
- Frontend touched: NO
- Yango clients/services touched: NO
- Recommended next phase: make operational-local reachable and rerun Phase 1E Retry
