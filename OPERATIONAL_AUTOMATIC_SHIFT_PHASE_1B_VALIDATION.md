# Operational Automatic Shift Phase 1B Validation

## 1. Objetivo de la fase

Validar en paralelo los turnos automaticos inferidos contra la realidad manual actual sin reemplazar el flujo manual ni alimentar liquidacion.

La fase compara:

- `operational_shift_sessions`
- `operational_trip_facts`
- `shift_sessions`
- `DriverClose` solo como lectura contextual de placa

## 2. Endpoints agregados

Base:

- `GET /api/pro-ops/operational-monitoring/validation`

Rutas:

- `GET /summary`
- `GET /manual-comparison`
- `GET /mismatches`
- `GET /coverage`

Todos son read-only.

## 3. Como funciona el matching

Reglas implementadas:

1. match primario por `driver_id`
2. comparacion temporal por apertura y solapamiento
3. tolerancia inicial:
   - apertura: `90` minutos
   - cierre: `90` minutos
4. cada turno manual matchea como maximo con un turno automatico
5. cada turno automatico matchea como maximo con un turno manual
6. si existe placa manual via `DriverClose`, se compara contra `vehicle_plate_normalized`
7. si no hay placa manual u operacional, la comparacion vehicular queda `NOT_AVAILABLE`

## 4. Metricas disponibles

En `summary`:

- conteos de turnos automaticos y manuales
- conteos de match y no match
- conteos de `operational_trip_facts`
- cobertura de `vehicle_key`
- porcentaje de `NEEDS_REVIEW`
- conteo de `AUTO_CLOSED_BY_NEXT_DRIVER`
- conteo de `STALE_CANDIDATE`
- deltas promedio y percentiles de apertura/cierre
- readiness de reemplazo manual solo como señal analitica

En `coverage`:

- viajes con y sin `vehicle_key`
- viajes con placa normalizada
- turnos por nivel de confianza
- turnos `NEEDS_REVIEW`

## 5. Formula de readiness

Valores posibles:

- `NOT_READY`
- `WATCH`
- `PROMISING`
- `READY_CANDIDATE`

Criterio aplicado:

- `NOT_READY` si falta data operacional o si cobertura/matching aun es bajo
- `WATCH` con cobertura >= 70, matching >= 60 y `needsReview` <= 20
- `PROMISING` con cobertura >= 85, matching >= 75 y `needsReview` <= 10
- `READY_CANDIDATE` con cobertura >= 95, matching >= 90, `needsReview` <= 5 y p95 de deltas <= 60

Aunque el resultado sea `READY_CANDIDATE`, esta fase no reemplaza el flujo manual.

## 6. Como interpretar resultados

- `MATCHED`: turno automatico y manual alinean de forma razonable
- `AUTO_ONLY`: no se encontro manual cercano para el turno automatico
- `MANUAL_ONLY`: no se encontro automatico cercano para el turno manual
- `TIME_DELTA_HIGH`: existe par, pero la diferencia temporal excede tolerancia
- `VEHICLE_MISMATCH`: misma persona y ventana temporal, pero la placa contextual discrepa
- `NEEDS_REVIEW`: el turno automatico ya venia con señal de revision
- `INSUFFICIENT_DATA`: falta cierre en uno de los lados y solo se pudo validar parcialmente

## 7. Que significa cada mismatch

- `AUTO_ONLY`: posible hueco del motor automatico o desfase de rango
- `MANUAL_ONLY`: posible falta de `operational_trip_facts` o inferencia incompleta
- `TIME_DELTA_HIGH`: apertura/cierre automatico lejos de la realidad manual
- `VEHICLE_MISMATCH`: posible problema de placa o asignacion contextual
- `NEEDS_REVIEW`: calidad insuficiente de datos operacionales
- `INSUFFICIENT_DATA`: comparacion incompleta por falta de cierre

## 8. Limitaciones

- la comparacion vehicular depende de placa en `DriverClose`; el turno manual no guarda vehiculo propio
- la validacion no llama Yango; solo usa data persistida
- el matching se calcula on-demand y esta pensado para rangos cortos
- `mirrorModeEnabled` sigue siendo una compuerta pendiente para writes internos futuros

## 9. Como validar 1 dia / 7 dias

Recomendacion:

- empezar con `from/to` de 1 dia
- luego ampliar a 3 o 7 dias
- revisar primero `coverage`, luego `summary`, luego `mismatches`

## 10. Evidencia necesaria antes de pensar en reemplazo manual

- alta cobertura de `vehicle_key`
- bajo porcentaje de `NEEDS_REVIEW`
- matching consistente contra sesiones manuales
- deltas temporales bajos y estables
- ausencia de mismatches sistematicos por conductor o placa

## 11. Confirmacion no-touch

Confirmado:

- no se escriben tablas manuales ni financieras
- no se toca `shift_sessions` como escritura
- no se toca `trips`
- no se toca `DriverClose`
- no se toca liquidacion
- no se toca frontend
- no se agregan llamadas Yango
- no se crea scheduler

## 12. Siguiente fase recomendada

Siguiente paso recomendado:

- observar metricas reales por 1 dia y 7 dias
- identificar patrones de mismatch
- si la evidencia acompana, recien despues plantear UI operacional de validacion
