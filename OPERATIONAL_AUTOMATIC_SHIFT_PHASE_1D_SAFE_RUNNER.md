# Operational Automatic Shift Phase 1D Safe Runner

## 1. Objetivo

Dejar un mecanismo interno, explicito y seguro para repetir la validacion operacional de Fase 1C con datos reales sin crear UI, sin reemplazar el flujo manual y sin tocar liquidacion.

El mecanismo preparado en esta fase permite:

- verificar si la migracion `017_operational_automatic_shift_mirror.sql` esta aplicada;
- confirmar si existen las tablas `operational_trip_facts`, `operational_shift_sessions` y `operational_shift_events`;
- habilitar un runner interno solo con properties explicitas;
- importar viajes por conductor y por un rango maximo de 1 dia;
- reprocesar turnos operacionales por conductor;
- consultar readiness de validacion por conductor;
- seleccionar una muestra pequena desde `shift_sessions` solo en modo read-only.

## 2. Por que 1C quedo `NO_OPERATIONAL_DATA`

La Fase 1C se cerro como `NO_OPERATIONAL_DATA` porque la base auditada no tenia tablas `operational_*` y el entorno no pudo clasificarse como local, dev o staging claramente seguro.

Resultado:

- no se ejecuto importacion;
- no se ejecuto reproceso;
- no se hicieron writes;
- no se forzo migracion automatica en una base compartida.

## 3. Como confirmar entorno seguro

El runner falla cerrado salvo que se cumplan todas estas condiciones:

- `operational.monitoring.runner.enabled=true`
- `operational.monitoring.runner.environment=local|dev|staging`
- `operational.monitoring.runner.confirm-writes-to-operational-tables=true`
- `operational.monitoring.runner.date-from=YYYY-MM-DD`
- `operational.monitoring.runner.date-to=YYYY-MM-DD`

Si el environment es `prod`, `production`, `unknown` o cualquier otro valor no permitido, el runner aborta.

## 4. Como aplicar o verificar migracion `017`

Diagnostico implementado:

- verifica existencia de `operational_trip_facts`;
- verifica existencia de `operational_shift_sessions`;
- verifica existencia de `operational_shift_events`;
- detecta si existe tabla de historial de migraciones conocida;
- informa el mecanismo observado en la repo.

Hallazgo actual de la repo:

- no hay dependencia Flyway;
- no hay dependencia Liquibase;
- existe el archivo `src/main/resources/db/migration/017_operational_automatic_shift_mirror.sql`;
- en `dev` existe `spring.jpa.hibernate.ddl-auto=update`;
- en `prod` existe `spring.jpa.hibernate.ddl-auto=validate`.

Interpretacion:

- el mecanismo real hoy no es Flyway operativo;
- la ruta segura preferida es aplicar `017_operational_automatic_shift_mirror.sql` manualmente en local/staging controlado;
- no confiar en Hibernate `update` como mecanismo de rollout operacional.

## 5. Como habilitar el runner

El runner es un `ApplicationRunner` interno y no expone endpoint HTTP.

Se activa solo si se define:

```properties
operational.monitoring.runner.enabled=true
operational.monitoring.runner.environment=staging
operational.monitoring.runner.confirm-writes-to-operational-tables=true
operational.monitoring.runner.date-from=2026-06-23
operational.monitoring.runner.date-to=2026-06-23
```

Opcionalmente se puede pasar una muestra explicita:

```properties
operational.monitoring.runner.driver-ids[0]=driver-a
operational.monitoring.runner.driver-ids[1]=driver-b
```

Si no se pasan `driverIds`, el runner usa un selector read-only sobre `shift_sessions` para tomar hasta 5 conductores unicos del dia solicitado.

## 6. Properties requeridas

- `operational.monitoring.runner.enabled`
- `operational.monitoring.runner.environment`
- `operational.monitoring.runner.confirm-writes-to-operational-tables`
- `operational.monitoring.runner.date-from`
- `operational.monitoring.runner.date-to`

Properties opcionales:

- `operational.monitoring.runner.driver-ids`
- `operational.monitoring.runner.vehicle-key`
- `operational.monitoring.runner.default-driver-count`
- `operational.monitoring.runner.max-driver-count`
- `operational.monitoring.runner.use-manual-sample-selector`

## 7. Limites de seguridad

Limites implementados:

- apagado por defecto;
- solo `local`, `dev` o `staging`;
- confirmacion de writes obligatoria;
- rango maximo por corrida: `1` dia;
- cantidad default de conductores: `5`;
- cantidad maxima absoluta de conductores: `20`;
- aborta si faltan tablas `operational_*`;
- aborta si la migracion `017` no parece aplicada por existencia de tablas.

## 8. Como correr muestra de 1 dia

Paso seguro sugerido:

1. clonar o confirmar una base local/staging aislada;
2. aplicar manualmente `017_operational_automatic_shift_mirror.sql` si las tablas no existen;
3. configurar las properties del runner;
4. dejar `date-from` y `date-to` en el mismo dia;
5. pasar hasta 5 conductores explicitos o dejar que el selector use `shift_sessions`.

Resultado esperado del runner:

- environment reportado;
- rango reportado;
- conductores solicitados y exitosos;
- `importedTripFacts`;
- `tripFactsConsidered`;
- `sessionsCreated`;
- `eventsCreated`;
- `needsReview`;
- `autoClosed`;
- `staleCandidate`;
- readiness por conductor si la validacion puede consultarse.

## 9. Como correr muestra de 7 dias

El guard actual no permite 7 dias en una sola corrida.

Forma segura en esta fase:

- repetir 7 corridas separadas de 1 dia cada una;
- mantener el mismo conjunto pequeno de conductores;
- revisar readiness por dia antes de ampliar cobertura.

No elevar el limite a 7 dias sin una decision explicita de hardening posterior.

## 10. Que tablas puede escribir

Solo debe escribir a traves de services operacionales ya existentes en:

- `operational_trip_facts`
- `operational_shift_sessions`
- `operational_shift_events`

## 11. Que tablas NO puede tocar

No debe tocar:

- `shift_sessions`
- `trips`
- `module_driver_closes`
- tablas de liquidacion
- tablas de pagos
- tablas de tiers
- frontend
- clientes/servicios Yango existentes

## 12. Rollback

Si se detecta un problema:

1. desactivar `operational.monitoring.runner.enabled=false`;
2. no volver a ejecutar el runner;
3. limpiar solo tablas `operational_*` en el entorno seguro si corresponde;
4. mantener intactas tablas manuales y financieras;
5. revertir el commit de esta fase si se decide retirar el mecanismo.

## 13. Como repetir Fase 1C

Secuencia recomendada:

1. confirmar entorno `local/dev/staging`;
2. confirmar migracion `017` aplicada;
3. ejecutar muestra de 1 dia;
4. revisar `summary`, `coverage`, `mismatches` y `manual-comparison`;
5. repetir 7 dias como 7 corridas acotadas;
6. documentar evidencia real en un nuevo reporte de Fase 1C.

## 14. Checklist antes de UI

- tablas `operational_*` existen;
- import controlado funciona;
- reproceso controlado funciona;
- coverage de `vehicle_key` es util;
- `needsReview` es aceptable;
- matching manual vs automatico tiene evidencia real;
- no se toco flujo manual;
- no se toco liquidacion;
- no se creo endpoint publico write;
- no se creo scheduler.
