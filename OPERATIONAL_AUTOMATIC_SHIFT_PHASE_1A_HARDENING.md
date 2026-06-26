# Operational Automatic Shift Phase 1A.6 Hardening

## 1. Objetivo

Corregir las observaciones de aceptacion de Fase 1A sin agregar logica de negocio nueva y manteniendo aislamiento total de la capa `operational_*`.

## 2. Observaciones corregidas

- se agrego paginacion segura con `limit` y `offset` a `GET /api/pro-ops/operational-monitoring/shifts`
- se agrego paginacion segura con `limit` y `offset` a `GET /api/pro-ops/operational-monitoring/events`
- se elimino `@CrossOrigin(origins = "*")` del controller operacional
- se aclaro el alcance real del cambio Yango

## 3. Limites aplicados a `/shifts`

- `limit` default: `200`
- `limit` maximo: `1000`
- `limit <= 0`: usa `200`
- `offset` null o negativo: usa `0`
- orden estable: `openedAt DESC`, `id DESC`
- filtros preservados: `from`, `to`, `driverId`, `vehicleKey`, `state`

## 4. Limites aplicados a `/events`

- `limit` default: `200`
- `limit` maximo: `1000`
- `limit <= 0`: usa `200`
- `offset` null o negativo: usa `0`
- orden estable: `eventTime DESC`, `id DESC`
- filtros preservados: `from`, `to`, `shiftId`, `driverId`, `vehicleKey`

## 5. Decision CORS

Se elimino `@CrossOrigin(origins = "*")` de `OperationalMonitoringController`.

Razon:

- la repo ya tiene manejo transversal de controllers abiertos en otras capas, pero esta fase prioriza no ampliar lectura cross-origin en una capa nueva y sensible;
- no fue necesario tocar configuracion global de seguridad/CORS;
- los endpoints permanecen read-only.

## 6. Decision `mirrorModeEnabled`

- no se bloqueo lectura GET con `mirrorModeEnabled`
- importacion y reproceso siguen siendo internos y no expuestos por API
- una compuerta operativa explicita para write operacional puede evaluarse en Fase 1B si hiciera falta

## 7. Aclaracion Yango

Confirmado:

- no se modificaron clientes Yango existentes
- no cambio autenticacion
- no cambio proxy o cookies
- no cambio paginacion remota
- no hay scheduler ni startup hook
- la capacidad nueva de importacion es interna y bajo demanda
- no existe endpoint publico write de importacion o reproceso

## 8. Tests y build ejecutados

- `mvn -DskipTests=false test`
- `mvn -DskipTests package`

## 9. No-touch verification

No se modificaron:

- `ShiftSession.java`
- `Trip.java`
- `LiquidacionServiceImpl.java`
- `DriverCloseServiceImpl.java`
- `FacturacionSemanalServiceImpl.java`
- `LiquidacionController.java`
- `ShiftSessionController.java`
- `DriverClose.java`
- `FacturacionSemanal.java`
- `PaymentPercentage.java`
- `BonusThreshold.java`
- migraciones existentes
- frontend
- clientes/servicios Yango existentes

## 10. Readiness para Fase 1B

Resultado:

- la capa queda lista para escalar lectura operativa con consultas acotadas
- permanece en modo espejo/read-only
- se mantiene la separacion respecto al flujo manual y financiero

## 11. Riesgos pendientes

- `mirrorModeEnabled` sigue siendo una propiedad documental para la capa read-only y no una compuerta fuerte de write interno
- el poblado real de `operational_trip_facts` sigue dependiendo de una invocacion interna controlada
