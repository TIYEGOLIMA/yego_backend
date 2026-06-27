# Operational Automatic Shift Phase 1E One-Day Real Run

## 1. Executive decision

ENVIRONMENT_NOT_SAFE

La Fase 1E no debe ejecutarse sobre el runtime actualmente configurado en esta repo porque el entorno no puede clasificarse de forma inequívoca como `local`, `dev` o `staging` aislado. La configuración activa por defecto usa perfil `dev`, pero sigue apuntando a una base PostgreSQL remota compartida con nombre de base `yego_integral`, lo que mantiene riesgo de entorno productivo o compartido.

Por seguridad:

- no se ejecutó el runner protegido;
- no se ejecutó importación Yango;
- no se ejecutó reproceso;
- no se aplicó la migración `017` sobre runtime;
- no se hicieron writes en tablas `operational_*`;
- no se tocaron tablas manuales ni financieras.

## 2. Pre-check

- Branch actual: `master`
- `git status`: limpio al inicio de la fase
- HEAD actual: `230075f`
- Commits requeridos confirmados en el historial:
  - `0d3a352` docs(pro-ops): report automatic shift phase 1c baseline
  - `f32c130` feat(pro-ops): add guarded operational shift validation runner
- Lecturas confirmadas:
  - `OPERATIONAL_AUTOMATIC_SHIFT_PHASE_1A.md`
  - `OPERATIONAL_AUTOMATIC_SHIFT_PHASE_1B_VALIDATION.md`
  - `OPERATIONAL_AUTOMATIC_SHIFT_PHASE_1C_REAL_DATA_VALIDATION.md`
  - `OPERATIONAL_AUTOMATIC_SHIFT_PHASE_1D_SAFE_RUNNER.md`
- Properties del runner implementadas:
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
- La repo marca `dev` como perfil Maven activo por defecto.
- El runner está apagado por defecto.

## 3. Environment safety decision

Clasificación final: `ENVIRONMENT_NOT_SAFE`

Hallazgos:

- `pom.xml` activa perfil `dev` por defecto, pero eso no basta para probar aislamiento real;
- `application.properties` apunta a una base PostgreSQL remota;
- `application-dev.yml` también apunta a la misma base remota y al mismo nombre de base;
- `application-prod.yml` reutiliza el mismo host y nombre base;
- el nombre de base observado es `yego_integral`, consistente con una base principal compartida;
- `operational.monitoring.runner.environment` queda en `unknown` por defecto;
- no hay evidencia documental en la repo de que este runtime sea un clon local o staging aislado solo para esta fase.

Decisión:

- no continuar a ejecución de runner;
- no continuar a migración runtime;
- no continuar a selección de ventana operativa sobre base real;
- limitar la fase a auditoría estática y reporte.

## 4. Migration check

Estado observado:

- el archivo `src/main/resources/db/migration/017_operational_automatic_shift_mirror.sql` existe en la repo;
- no hay dependencia Flyway en `pom.xml`;
- no hay dependencia Liquibase en `pom.xml`;
- el mecanismo real no es un rollout gestionado por Flyway/Liquibase;
- Fase 1D ya documentó que la vía segura preferida es aplicación manual en `local/staging` controlado.

Decisión de esta fase:

- no verificar tablas `operational_*` contra la base runtime;
- no aplicar migración `017` sobre la base runtime;
- no modificar la migración;
- no crear migración nueva.

Resultado:

- migración presente en código: SÍ
- migración aplicada en runtime seguro: NO verificada
- gate de migración no ejecutado por bloqueo previo de seguridad.

## 5. Operational window selected

No seleccionada.

Motivo:

- al fallar el Gate 1 no corresponde inspeccionar ni preparar una corrida real contra la base runtime compartida;
- no es seguro derivar una ventana operativa de 24 horas desde un entorno no clasificado como aislado.

Resultado:

- `operational_window`: no ejecutada
- `calendar_day`: no ejecutado
- `shift_type` objetivo: no evaluado

## 6. Manual baseline selected

No seleccionado para ejecución de Fase 1E.

Notas:

- la Fase 1C ya dejó evidencia de que existe baseline manual útil en `shift_sessions` y `DriverClose`;
- esa evidencia no alcanza para autorizar una corrida 1E sobre el runtime actual;
- la selección real de hasta 5 conductores debe ocurrir recién en una base `local/dev/staging` inequívocamente segura.

## 7. Runner command and properties

No ejecutado.

Estado del runner:

- `ApplicationRunner` interno protegido por `@ConditionalOnProperty`;
- `enabled=false` por defecto;
- `environment=unknown` por defecto;
- guard admite solo `local`, `dev` o `staging`;
- guard exige confirmación explícita de writes;
- guard exige `date-from` y `date-to`;
- guard impone rango máximo de 1 día;
- guard impone límite de conductores.

Comando real usado:

- ninguno, porque el Gate 1 falló antes de cualquier activación.

Properties no sensibles que habrían sido obligatorias:

- `operational.monitoring.runner.enabled=true`
- `operational.monitoring.runner.environment=local|dev|staging`
- `operational.monitoring.runner.confirm-writes-to-operational-tables=true`
- `operational.monitoring.runner.date-from=YYYY-MM-DD`
- `operational.monitoring.runner.date-to=YYYY-MM-DD`

## 8. Import results

No ejecutado.

Resultado:

- `Yango calls executed`: NO
- `import result`: `IMPORT_NOT_EXECUTED_FOR_SAFETY`
- `operational_trip_facts` creados/upserted: `0`
- errores por conductor: no aplica

## 9. Reprocess results

No ejecutado.

Resultado:

- `reprocess result`: `REPROCESS_NOT_EXECUTED_FOR_SAFETY`
- `operational_shift_sessions` creadas: `0`
- `operational_shift_events` creados: `0`

## 10. Validation coverage

No ejecutada.

No se consultaron endpoints read-only de validación porque la fase se cerró antes de generar evidencia operacional real en un entorno seguro.

Métricas:

- `operationalTripFactCount`: no disponible
- `tripFactsWithVehicleKeyCount`: no disponible
- `tripFactsMissingVehicleKeyCount`: no disponible
- `vehicleKeyCoveragePct`: no disponible
- `operationalShiftCount`: no disponible
- `needsReviewShiftCount`: no disponible
- `needsReviewShiftPct`: no disponible

## 11. Validation summary

No ejecutada.

Métricas:

- `manualShiftCount`: no disponible en esta fase
- `matchedShiftCount`: no disponible
- `unmatchedOperationalShiftCount`: no disponible
- `unmatchedManualShiftCount`: no disponible
- `autoClosedByNextDriverCount`: no disponible
- `staleCandidateCount`: no disponible
- `averageStartDeltaMinutes`: no disponible
- `averageEndDeltaMinutes`: no disponible
- `p95StartDeltaMinutes`: no disponible
- `p95EndDeltaMinutes`: no disponible
- `manualReplacementReadiness`: `NOT_READY`

## 12. Manual comparison findings

No hay hallazgos nuevos de comparación manual vs automática porque:

- no hubo importación operacional;
- no hubo reproceso operacional;
- no hubo `manual-comparison` posterior a una corrida segura.

## 13. Mismatch findings

No hay mismatches nuevos de Fase 1E.

Resultado:

- `top mismatch types`: no disponible
- `main mismatch type`: no disponible

## 14. Vehicle key quality

No se puede medir calidad real de `vehicle_key` en esta fase.

Sí queda confirmado:

- la lógica de calidad/cobertura existe en backend;
- la medición depende de poblar `operational_trip_facts` en una corrida segura;
- sin esa corrida, cualquier lectura de cobertura sería especulativa.

## 15. Day/night/cross-midnight analysis

No evaluado con data real en esta fase.

Conclusión conservadora:

- no se observó formalmente turno diurno;
- no se observó formalmente turno nocturno;
- no se observó formalmente cruce de medianoche;
- no puede descartarse un problema de corte por fecha calendario;
- sigue siendo necesario validar una ventana operativa corrida de 24 horas en entorno seguro.

## 16. Sensitivity configuration learnings

No se implementa nada en esta fase, pero la necesidad conceptual sigue vigente.

Propuesta futura no implementada:

`operational_shift_sensitivity_config`

Campos conceptuales recomendados:

- `park_id`
- `city`
- `day_shift_start`
- `day_shift_end`
- `night_shift_start`
- `night_shift_end`
- `opening_event`
- `opening_grace_minutes`
- `closing_event`
- `closing_grace_minutes`
- `stale_candidate_minutes`
- `max_gap_same_shift_minutes`
- `cross_midnight_policy`
- `auto_close_on_next_driver`
- `needs_review_threshold_minutes`
- `effective_from`
- `enabled`

Aprendizaje:

- la Fase 1E no debe intentar sensibilidad avanzada sin antes asegurar entorno y evidencia real mínima;
- la validación día/noche y cruce de medianoche sigue siendo requisito clave antes de cualquier UI operacional seria.

## 17. Feedback module learnings

No se implementa módulo ni tabla, pero la futura revisión humana debería capturar al menos:

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

Campos conceptuales futuros:

- `operational_shift_id`
- `manual_shift_id`
- `driver_id`
- `vehicle_key`
- `feedback_type`
- `operator_comment`
- `suggested_opened_at`
- `suggested_closed_at`
- `suggested_shift_type`
- `created_by`
- `created_at`

## 18. Operational risks found

- el runtime configurado apunta a base remota compartida;
- el nombre de base y la configuración no prueban aislamiento;
- el perfil `dev` por defecto no garantiza seguridad operacional;
- el runner está correctamente blindado, pero el entorno base no supera el Gate 1;
- ejecutar import/reprocess aquí podría contaminar una base no aislada;
- el proyecto aún tiene secretos embebidos en archivos de configuración, lo que aumenta riesgo operacional y de gobernanza.

## 19. UI readiness decision

`NOT_READY`

Motivo:

- no existe evidencia real mínima de una corrida 1E segura;
- no hay métricas reales nuevas de coverage, summary o mismatches;
- no se validó comportamiento diurno/nocturno ni cruce de medianoche.

## 20. Manual replacement readiness decision

`NOT_READY`

Decisión conservadora obligatoria:

- no aprobar reemplazo manual en esta fase;
- no interpretar la existencia del runner como permiso para reemplazar `shift_sessions`.

## 21. Required fixes before 7-day run

- crear o confirmar una base `local`, `dev` o `staging` realmente aislada;
- eliminar cualquier ambigüedad de entorno compartido/producción;
- aplicar `017_operational_automatic_shift_mirror.sql` en ese entorno seguro;
- verificar existencia de `operational_trip_facts`, `operational_shift_sessions` y `operational_shift_events`;
- correr primero una muestra de 1 día con máximo 5 conductores;
- solo si esa muestra es segura y útil, repetir 7 corridas separadas de 1 día.

## 22. Required fixes before UI

- superar Fase 1E con evidencia real mínima;
- medir coverage real de `vehicle_key`;
- medir `matchedShiftPct`;
- medir `needsReviewShiftPct`;
- revisar mismatches dominantes;
- validar turnos diurnos, nocturnos y casos cruzando medianoche;
- documentar un criterio estable de ventana operativa.

## 23. Required fixes before feedback/sensitivity module

- asegurar un entorno seguro para recolección de evidencia;
- obtener al menos una corrida útil con mismatches revisables;
- definir reglas iniciales de clasificación `DAY/NIGHT/UNKNOWN`;
- documentar dónde el motor abre/cierra demasiado temprano o tarde;
- separar claramente problemas de data Yango vs problemas de inferencia;
- extraer casos reales para alimentar catálogo de feedback.

## 24. No-touch verification

Confirmado en esta fase:

- no se modificó código backend funcional;
- no se crearon endpoints nuevos;
- no se creó scheduler;
- no se tocaron tablas manuales;
- no se tocó liquidación;
- no se tocaron pagos;
- no se tocaron tiers;
- no se tocaron gastos;
- no se tocó frontend;
- no se tocaron clientes/servicios Yango existentes;
- no se crearon migraciones nuevas;
- no se alteraron tablas existentes.

## 25. Final recommendation

No ejecutar Fase 1E en el runtime actualmente configurado en la repo.

Siguiente paso recomendado:

1. preparar una base inequívocamente aislada `local/dev/staging`;
2. aplicar manualmente `017_operational_automatic_shift_mirror.sql`;
3. configurar explícitamente el runner con `enabled=true`, `environment` permitido y confirmación de writes;
4. seleccionar una ventana operativa de 24 horas con posibilidad de observar día/noche y cruce de medianoche;
5. ejecutar recién allí la primera corrida real controlada.

VERDICT:

- Executive decision: ENVIRONMENT_NOT_SAFE
- Branch: master
- Commit: 230075f
- Environment: UNKNOWN_FOR_SAFE_EXECUTION
- Migration 017 applied/existed: NO
- Operational tables existed before run: NO
- Operational tables existed after migration: NO
- Runner executed: NO
- Runner guard passed: NO
- Operational window tested: NONE
- Shift types observed: NONE
- Cross-midnight observed: NO
- Drivers tested: 0
- Yango calls executed: NO
- Yango call scope: NONE
- Import result: IMPORT_NOT_EXECUTED_FOR_SAFETY
- Reprocess result: REPROCESS_NOT_EXECUTED_FOR_SAFETY
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
- Files changed: OPERATIONAL_AUTOMATIC_SHIFT_PHASE_1E_ONE_DAY_REAL_RUN.md
- Migrations created: NO
- Existing tables altered: NO
- Financial logic touched: NO
- `shift_sessions` touched: NO
- `trips` touched: NO
- `DriverClose` touched: NO
- Frontend touched: NO
- Yango clients/services touched: NO
- Recommended next phase: prepare isolated environment and rerun Phase 1E safely
