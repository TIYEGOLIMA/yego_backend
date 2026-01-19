# Sistema de Pagos Basado en Turnos Generados - Yego Pro-Ops

## 📋 Índice
1. [Contexto General](#contexto-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Estructura de Datos](#estructura-de-datos)
4. [Endpoints Necesarios](#endpoints-necesarios)
5. [Flujo de Trabajo](#flujo-de-trabajo)
6. [Vista Frontend](#vista-frontend)
7. [Implementación Backend](#implementación-backend)

---

## 🎯 Contexto General

El sistema **Yego Pro-Ops** genera automáticamente turnos (diurnos y nocturnos) para cada conductor basándose en su actividad real de viajes. Estos turnos se calculan diariamente mediante un scheduler que procesa el día anterior.

**Objetivo:** Crear un sistema de pagos que permita:
- Ver todos los conductores con turnos generados pendientes de pago
- **Registrar un pago por cada turno individual** (1 turno = 1 pago)
- Mantener historial de pagos realizados
- Gestionar el estado de cada pago (pendiente/pagado/cancelado)

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    SCHEDULER (18:42)                         │
│  Calcula y guarda turnos del día anterior automáticamente   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              CalculatedShift (Turnos Generados)             │
│  - driver_id                                                │
│  - fecha                                                     │
│  - tipo_turno (diurno/nocturno)                            │
│  - hora_inicio                                              │
│  - hora_fin                                                 │
│  - duracion_minutos                                         │
│  - estado (activo/finalizado)                              │
│  - es_manual (false = generado por scheduler)              │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Payment (Pagos Registrados)                    │
│  - id                                                       │
│  - turno_id (FK a CalculatedShift) - UN TURNO = UN PAGO   │
│  - driver_id                                               │
│  - fecha_pago                                               │
│  - monto_total                                              │
│  - estado_pago (pendiente/pagado/cancelado)                │
│  - observaciones                                            │
│  - created_at / updated_at                                 │
└──────────────────────────────────────────────────────────────┘
```

---

## 📊 Estructura de Datos

### 1. Entidad: `Payment` (Nuevo)

```java
@Entity
@Table(name = "module_payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "turno_id", nullable = false, unique = true)
    private CalculatedShift turno; // UN TURNO = UN PAGO (relación 1:1)
    
    @Column(name = "driver_id", nullable = false)
    private String driverId; // Redundante pero útil para consultas
    
    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;
    
    @Column(name = "monto_total", nullable = false)
    private BigDecimal montoTotal;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    private EstadoPago estadoPago; // PENDIENTE, PAGADO, CANCELADO
    
    @Column(name = "observaciones", length = 1000)
    private String observaciones;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum EstadoPago {
        PENDIENTE, PAGADO, CANCELADO
    }
}
```

### 2. DTOs de Respuesta

```java
// Lista de conductores con turnos pendientes de pago
public class ConductorConTurnosPendientesResponse {
    private String driverId;
    private String driverName;
    private String vehicleNumber;
    private List<TurnoPendienteInfo> turnosPendientes;
    private Integer totalTurnos;
    private Integer totalMinutos;
    private BigDecimal montoEstimado; // Opcional: calcular basado en tarifa
    private boolean tienePagoPendiente;
}

public class TurnoPendienteInfo {
    private Long turnoId;
    private LocalDate fecha;
    private String tipoTurno; // "diurno" o "nocturno"
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private Integer duracionMinutos;
    private CalculatedShift.EstadoTurno estado;
}

// Request para crear pago (UN TURNO = UN PAGO)
public class CrearPagoRequest {
    private Long turnoId; // ID del turno a pagar (único)
    private LocalDate fechaPago;
    private BigDecimal montoTotal;
    private String observaciones;
}

public class PagoResponse {
    private Long id;
    private Long turnoId;
    private String driverId;
    private String driverName;
    private LocalDate fechaPago;
    private BigDecimal montoTotal;
    private EstadoPago estadoPago;
    private TurnoPendienteInfo turnoAsociado; // Un solo turno
    private String observaciones;
    private LocalDateTime createdAt;
}
```

---

## 🔌 Endpoints Necesarios

### 1. **GET** `/api/pro-ops/payments/conductores-pendientes`
Obtiene lista de conductores con turnos generados pendientes de pago.

**Query Parameters:**
- `fechaDesde` (opcional): Filtrar desde fecha
- `fechaHasta` (opcional): Filtrar hasta fecha
- `soloConPendientes` (opcional, default: true): Solo conductores con turnos sin pagar

**Response:**
```json
{
  "conductores": [
    {
      "driverId": "73ffe5abf820442697122497c6b48d1a",
      "driverName": "Juan Pérez",
      "vehicleNumber": "CLI234",
      "turnosPendientes": [
        {
          "turnoId": 769,
          "fecha": "2026-01-13",
          "tipoTurno": "nocturno",
          "horaInicio": "2026-01-13T20:56:23",
          "horaFin": "2026-01-14T06:16:14",
          "duracionMinutos": 559,
          "estado": "finalizado",
          "tienePago": false,
          "pagoId": null
        },
        {
          "turnoId": 770,
          "fecha": "2026-01-14",
          "tipoTurno": "nocturno",
          "horaInicio": "2026-01-14T21:22:08",
          "horaFin": "2026-01-15T05:01:33",
          "duracionMinutos": 459,
          "estado": "finalizado",
          "tienePago": false,
          "pagoId": null
        }
      ],
      "totalTurnos": 2,
      "totalMinutos": 1018,
      "montoEstimado": 850.50,
      "turnosPendientes": 2,
      "turnosPagados": 0
    }
  ],
  "total": 15
}
```

### 2. **GET** `/api/pro-ops/payments/turnos-pendientes/{driverId}`
Obtiene todos los turnos pendientes de pago de un conductor específico.

**Response:**
```json
{
  "driverId": "73ffe5abf820442697122497c6b48d1a",
  "driverName": "Juan Pérez",
  "turnosPendientes": [...],
  "totalTurnos": 2,
  "totalMinutos": 1018
}
```

### 3. **POST** `/api/pro-ops/payments/crear`
Crea un nuevo pago para un turno específico (1 turno = 1 pago).

**Request Body:**
```json
{
  "turnoId": 769,
  "fechaPago": "2026-01-16",
  "montoTotal": 425.25,
  "observaciones": "Pago por turno nocturno del 13 de enero"
}
```

**Response:**
```json
{
  "id": 1,
  "turnoId": 769,
  "driverId": "73ffe5abf820442697122497c6b48d1a",
  "driverName": "Juan Pérez",
  "fechaPago": "2026-01-16",
  "montoTotal": 425.25,
  "estadoPago": "PENDIENTE",
  "turnoAsociado": {
    "turnoId": 769,
    "fecha": "2026-01-13",
    "tipoTurno": "nocturno",
    "horaInicio": "2026-01-13T20:56:23",
    "horaFin": "2026-01-14T06:16:14",
    "duracionMinutos": 559,
    "estado": "finalizado"
  },
  "observaciones": "Pago por turno nocturno del 13 de enero",
  "createdAt": "2026-01-16T10:30:00"
}
```

### 4. **PUT** `/api/pro-ops/payments/{paymentId}/marcar-pagado`
Marca un pago como pagado.

**Response:**
```json
{
  "id": 1,
  "estadoPago": "PAGADO",
  "updatedAt": "2026-01-16T11:00:00"
}
```

### 5. **GET** `/api/pro-ops/payments/historial/{driverId}`
Obtiene historial de pagos de un conductor.

**Query Parameters:**
- `fechaDesde` (opcional)
- `fechaHasta` (opcional)
- `estadoPago` (opcional): PENDIENTE, PAGADO, CANCELADO

**Response:**
```json
{
  "pagos": [
    {
      "id": 1,
      "fechaPago": "2026-01-16",
      "montoTotal": 425.25,
      "estadoPago": "PAGADO",
      "turnoAsociado": {
        "turnoId": 769,
        "fecha": "2026-01-13",
        "tipoTurno": "nocturno",
        "duracionMinutos": 559
      },
      "createdAt": "2026-01-16T10:30:00"
    }
  ],
  "total": 1
}
```

---

## 🔄 Flujo de Trabajo

### 1. **Generación Automática de Turnos**
```
Scheduler (18:42) → Calcula turnos del día anterior → Guarda en CalculatedShift
```

### 2. **Visualización de Pendientes**
```
Frontend → GET /conductores-pendientes → Muestra lista de conductores con turnos sin pagar
```

### 3. **Registro de Pago Individual**
```
Usuario selecciona turno → Click en "Pagar Turno" → Modal con datos del turno → Crea pago (1 turno = 1 pago)
```

### 4. **Registro de Pago**
```
POST /crear → Valida que turno no tenga pago → Crea Payment (1:1 con turno) → Retorna pago creado
```

### 5. **Marcar como Pagado**
```
PUT /marcar-pagado → Actualiza estado → Marca turnos como pagados (opcional: campo adicional)
```

---

## 🎨 Vista Frontend

### Estructura de Componentes

```
PagosTurnosPage
├── FiltrosBar
│   ├── FechaDesde
│   ├── FechaHasta
│   └── BotónBuscar
├── ListaConductores
│   └── ConductorCard
│       ├── InfoConductor (nombre, vehículo)
│       ├── ResumenTurnos (total turnos, minutos, monto total)
│       └── ListaTurnosPendientes
│           └── TurnoItem
│               ├── InfoTurno (fecha, tipo, duración, horas)
│               ├── EstadoPago (Pendiente/Pagado)
│               └── BotonPagar (si está pendiente)
└── ModalCrearPago
    ├── InfoTurno (resumen del turno a pagar)
    ├── FormularioPago
    │   ├── FechaPago
    │   ├── MontoTotal
    │   └── Observaciones
    └── Botones (Cancelar, Confirmar)
```

### Estados de la Vista

1. **Carga inicial:** Muestra lista de conductores con turnos pendientes
2. **Selección de turno:** Usuario hace click en "Pagar" de un turno específico
3. **Creación:** Modal para ingresar datos del pago (monto, fecha, observaciones)
4. **Confirmación:** Pago creado, actualizar estado del turno a "Pagado"

---

## 💻 Implementación Backend

### 1. **Service: PaymentService**

```java
@Service
public interface PaymentService {
    // Obtener conductores con turnos pendientes
    List<ConductorConTurnosPendientesResponse> obtenerConductoresPendientes(
        LocalDate fechaDesde, LocalDate fechaHasta);
    
    // Obtener turnos pendientes de un conductor
    ConductorConTurnosPendientesResponse obtenerTurnosPendientes(String driverId);
    
    // Crear pago para un turno (1 turno = 1 pago)
    PagoResponse crearPago(CrearPagoRequest request);
    
    // Marcar pago como pagado
    PagoResponse marcarComoPagado(Long paymentId);
    
    // Obtener historial de pagos
    List<PagoResponse> obtenerHistorialPagos(String driverId, 
        LocalDate fechaDesde, LocalDate fechaHasta, EstadoPago estado);
}
```

### 2. **Lógica de Consulta: Turnos Pendientes**

```sql
-- Turnos generados por scheduler que NO tienen un pago asociado
SELECT cs.*
FROM module_calculated_shifts cs
LEFT JOIN module_payments mp ON mp.turno_id = cs.id
WHERE cs.driver_id = ?
  AND cs.es_manual = false
  AND mp.id IS NULL
ORDER BY cs.fecha DESC, cs.hora_inicio;
```

### 3. **Validaciones al Crear Pago**

- ✅ Verificar que el `turnoId` exista
- ✅ Verificar que el turno NO tenga ya un pago asociado (relación 1:1)
- ✅ Verificar que `montoTotal` sea mayor a 0
- ✅ Verificar que `fechaPago` sea válida
- ✅ Verificar que el turno esté en estado "finalizado" (opcional)

### 4. **Marcar Turnos como Pagados (Opcional)**

Si quieres marcar los turnos como "pagados", puedes:
- **Opción A:** Agregar campo `pagado` a `CalculatedShift`
- **Opción B:** Solo verificar si el turno está en algún pago con estado PAGADO

---

## 📝 Script SQL para Crear Tabla

```sql
CREATE TABLE module_payments (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL UNIQUE, -- UN TURNO = UN PAGO (relación 1:1)
    driver_id VARCHAR(255) NOT NULL,
    fecha_pago DATE NOT NULL,
    monto_total DECIMAL(10, 2) NOT NULL,
    estado_pago VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    observaciones VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_turno FOREIGN KEY (turno_id) 
        REFERENCES module_calculated_shifts(id) ON DELETE CASCADE,
    CONSTRAINT chk_estado_pago CHECK (estado_pago IN ('PENDIENTE', 'PAGADO', 'CANCELADO'))
);

CREATE UNIQUE INDEX idx_payments_turno_id ON module_payments(turno_id);
CREATE INDEX idx_payments_driver_id ON module_payments(driver_id);
CREATE INDEX idx_payments_fecha_pago ON module_payments(fecha_pago);
CREATE INDEX idx_payments_estado ON module_payments(estado_pago);
```

---

## 🚀 Pasos de Implementación

### Backend:
1. ✅ Crear entidad `Payment`
2. ✅ Crear `PaymentRepository`
3. ✅ Crear `PaymentService` e implementación
4. ✅ Crear `PaymentController` con endpoints
5. ✅ Crear DTOs (Request/Response)
6. ✅ Implementar lógica de consulta de turnos pendientes
7. ✅ Implementar validaciones al crear pago

### Frontend:
1. ✅ Crear página `PagosTurnosPage`
2. ✅ Crear componente `ListaConductores`
3. ✅ Crear componente `ConductorCard` con turnos
4. ✅ Crear modal `ModalCrearPago`
5. ✅ Integrar con API endpoints
6. ✅ Agregar filtros y búsqueda

---

## 💡 Consideraciones Adicionales

1. **Cálculo de Monto:** Puedes calcular automáticamente el monto basado en:
   - Tarifa por minuto (diferente para diurno/nocturno)
   - Tarifa fija por turno
   - Tarifa variable según duración

2. **Historial:** Mantener historial completo de pagos para auditoría

3. **Notificaciones:** Enviar notificación WebSocket cuando se crea un pago

4. **Reportes:** Generar reportes de pagos por período

5. **Exportación:** Exportar lista de pagos a Excel/PDF

---

## 📌 Notas Importantes

- **Los turnos generados por el scheduler tienen `es_manual = false`**
- **UN TURNO = UN PAGO (relación 1:1)** - No se pueden agrupar múltiples turnos
- **Cada turno tiene su propio pago individual**
- El estado del pago es independiente del estado del turno
- La relación es única: `turno_id` tiene constraint UNIQUE en la tabla `module_payments`
- Si un turno ya tiene un pago, no se puede crear otro para ese turno

---

¿Quieres que implemente alguna parte específica de esto?

