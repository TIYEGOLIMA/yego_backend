# Operational Automatic Shift Phase 1A

## 1. Que se implemento

Se implemento una capa nueva y aislada de monitoreo operacional en modo espejo/read-only para inferir turnos automaticos sin tocar `shift_sessions`, `trips` ni la logica financiera existente.

Incluye:

- persistencia propia de hechos operativos;
- persistencia propia de turnos inferidos;
- persistencia propia de eventos de auditoria;
- resolucion de `vehicle_key`;
- upsert idempotente por `external_trip_id`;
- servicio de reproceso interno por rango de fechas;
- endpoints read-only de inspeccion;
- pruebas unitarias de resolucion, upsert e inferencia.

## 2. Tablas nuevas

Migracion nueva:

- `017_operational_automatic_shift_mirror.sql`

Tablas nuevas:

- `operational_trip_facts`
- `operational_shift_sessions`
- `operational_shift_events`

No se altero ninguna tabla existente.

## 3. Endpoints nuevos

Base:

- `GET /api/pro-ops/operational-monitoring`

Rutas:

- `GET /trip-facts`
- `GET /shifts`
- `GET /events`

Hardening Fase 1A.6:

- `GET /shifts` usa `limit` y `offset` con `limit` default `200` y maximo `1000`;
- `GET /events` usa `limit` y `offset` con `limit` default `200` y maximo `1000`;
- la paginacion aplica en consulta de repositorio, no en memoria;
- el controller operacional deja de declarar `@CrossOrigin("*")` y usa la configuracion global existente.

No se expuso `POST /reprocess` porque la configuracion actual de seguridad deja `pro-ops` abierto y esta fase no debe abrir un write endpoint sensible sin una compuerta clara de autorizacion.

## 4. Algoritmo de inferencia

1. Leer `operational_trip_facts` del rango solicitado.
2. Filtrar solo estados completados. El valor activo por defecto es `complete`.
3. Usar `booked_at` como timestamp principal.
4. Usar `ended_at` como fallback cuando `booked_at` falte y bajar la confianza.
5. Agrupar por `vehicle_key`.
6. Ordenar por `vehicle_key`, timestamp operativo y `external_trip_id`.
7. Abrir `OPEN_INFERRED` con el primer viaje valido.
8. Si el siguiente viaje es del mismo conductor, continuar el turno.
9. Si el siguiente viaje es de otro conductor en el mismo `vehicle_key`, cerrar el turno previo como `AUTO_CLOSED_BY_NEXT_DRIVER` y abrir uno nuevo.
10. Si un turno abierto queda sin nueva actividad por encima del umbral configurado dentro del rango reprocesado, marcarlo `STALE_CANDIDATE`.
11. Si falta `vehicle_key`, abrir o continuar una sesion `NEEDS_REVIEW`.
12. Registrar eventos de auditoria por cada decision importante.

## 5. Estados de turnos

Estados activos en esta fase:

- `OPEN_INFERRED`
- `AUTO_CLOSED_BY_NEXT_DRIVER`
- `STALE_CANDIDATE`
- `NEEDS_REVIEW`

No implementado todavia:

- `MANUALLY_CLOSED`
- `OPEN_MANUAL`
- integracion financiera

## 6. Definicion de `vehicle_key`

Orden de prioridad:

1. `car_id` estable de Yango cuando llega resuelto.
2. placa normalizada como fallback.
3. `UNRESOLVED` si no existe ninguno.

Normalizacion de placa:

- trim;
- uppercase;
- remover espacios;
- remover guiones;
- remover separadores comunes.

## 7. Limitaciones actuales

- No existe todavia una union garantizada entre cada viaje externo y un `car_id` estable en todos los casos.
- No se creo tabla `operational_vehicle_aliases` en esta fase para evitar una reconciliacion insegura.
- `LATE_TRIP_DETECTED` se cubre a nivel de inconsistencia temporal interna del reproceso, pero no reemplaza una estrategia futura mas fuerte de observacion incremental.
- No hay endpoint de reproceso por seguridad; el reproceso queda como servicio interno/test.
- La capa sigue siendo read-only respecto al flujo manual y financiero.
- `mirrorModeEnabled` sigue sin bloquear endpoints GET y no expone write API en esta fase.

## 8. Como probar

Backend:

- `mvn test`
- `mvn clean package`

Inspeccion manual sugerida:

- poblar `operational_trip_facts` con import interno o fixtures;
- ejecutar el reproceso por servicio en tests o desde una llamada interna controlada;
- consultar `GET /trip-facts`, `GET /shifts` y `GET /events`.

## 9. Como reprocesar

Servicio:

- `OperationalShiftInferenceService.reprocessRange(from, to, driverId, vehicleKey)`

Comportamiento:

- limpia sesiones/eventos previos del rango en la capa `operational_*`;
- vuelve a inferir desde `operational_trip_facts`;
- no toca tablas manuales ni financieras.

## 10. Como validar contra turnos manuales

Comparar:

- `operational_shift_sessions.opened_at` vs aperturas observadas;
- `closed_at` auto inferido vs cambio real de conductor;
- `needs_review` y `review_reason` vs outliers manuales;
- cobertura de `vehicle_key` resuelto vs cierres manuales actuales.

## 11. Confirmacion de aislamiento financiero

Confirmado:

- no se escribe en `shift_sessions`;
- no se escribe en `trips`;
- no se escribe en `DriverClose`;
- no se escribe en `FacturacionSemanal`;
- no se escribe en pagos, tiers, gastos ni deducciones;
- no se modifica frontend.

## 12. Riesgos restantes

- huecos de `vehicle_key` cuando solo hay placa o no hay vehiculo;
- eventos tardios con observacion incremental futura;
- dependencia de la calidad temporal del source Yango;
- ausencia de reconciliacion historica fuerte placa -> `car_id`.
- la capacidad de importacion Yango sigue siendo interna y bajo demanda; no cambia autenticacion, cookies, proxy, paginacion remota ni agrega scheduler.

## 13. Siguiente fase recomendada

Fase recomendada:

- validacion paralela contra turnos manuales reales;
- medicion de cobertura de `vehicle_key`;
- definicion segura de un mecanismo de reproceso interno protegido;
- si la evidencia acompana, luego UI operacional y reconciliacion con sesiones manuales.

## Rollback

1. revertir el commit de esta fase;
2. eliminar tablas `operational_*` en entorno de prueba si la migracion ya fue aplicada;
3. confirmar que liquidacion no depende de estas tablas;
4. confirmar que frontend y procesos manuales siguen intactos.
