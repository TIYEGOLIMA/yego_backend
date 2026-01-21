package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.DriverCloseRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.MultipleDriversTripsRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverSimpleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverPaymentSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.PaidShiftsResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.FechasConTiposTurnoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MultipleDriversTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.entities.CalculatedShift;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import com.yego.backend.service.yego_pro_ops.DriverCloseService;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/pro-ops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FleetDriverController {
    
    private final FleetDriverService fleetDriverService;
    private final DriverOrdersService driverOrdersService;
    private final DriverCloseService driverCloseService;
    private final CalculatedShiftService calculatedShiftService;
    
    /**
     * 📋 VISTA: DetalleView
     * Obtiene solo los viajes completos con atributos específicos (distancia, efectivo, tarjeta, precio)
     * O devuelve el cierre de caja si ya existe un registro para esa fecha
     * Soporta paginación mediante cursor para cargar más resultados
     * @param driverId ID del conductor
     * @param dateFrom Fecha inicial (formato: "2025-12-10T00:00:00-05:00")
     * @param dateTo Fecha final (formato: "2025-12-10T23:59:59-05:00")
     * @param cursor Cursor opcional para paginación (obtenido de la respuesta anterior)
     * @return Respuesta con lista de viajes completos y cursor para la siguiente página (si existe)
     *         O DriverCloseResponse si ya existe un cierre para esa fecha
     */
    @GetMapping("/driver/viajes-completos")
    public ResponseEntity<DriverOrdersResponse> obtenerViajesCompletos(
            @RequestParam String driverId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String cursor) {
        log.info("🚗 [FleetDriverController] Obteniendo viajes completos para driver_id: {}, desde: {}, hasta: {}, cursor: {}", 
            driverId, dateFrom, dateTo, cursor != null ? "presente" : "no presente");
        
        DriverOrdersResponse response = driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo, cursor);
        log.info("✅ [FleetDriverController] Total de viajes completos devueltos: {}, hasMore: {}, cierreRegistrado: {}", 
            response.getOrders().size(), response.getHasMore(), response.getCierreRegistrado());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 🔧 USO INTERNO: WebSocket (FleetDriverNotificationHandler)
     * Obtiene viajes simplificados para múltiples conductores en una sola llamada
     * Se usa para enviar datos por WebSocket, NO directamente desde las vistas del frontend
     * @param request Request con driver_ids, date_from y date_to
     * @return Respuesta con viajes agrupados por conductor
     */
    @PostMapping("/drivers/viajes-simplificados")
    public ResponseEntity<MultipleDriversTripsSimplifiedResponse> obtenerViajesSimplificadosMultiples(
            @Valid @RequestBody MultipleDriversTripsRequest request) {
        log.info("🚗 [FleetDriverController] Obteniendo viajes simplificados para {} conductores, desde: {}, hasta: {}", 
            request.getDriverIds().size(), request.getDateFrom(), request.getDateTo());
        
        MultipleDriversTripsSimplifiedResponse response = driverOrdersService.obtenerViajesSimplificadosMultiples(
            request.getDriverIds(), request.getDateFrom(), request.getDateTo());
        log.info("✅ [FleetDriverController] Total de conductores procesados: {}", 
            response.getDrivers().size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 🚗 VISTA: MonitoreoEnVivoView
     * Obtiene viajes simplificados para un conductor en una fecha específica
     * Solo requiere driver_id y fecha (formato: YYYY-MM-DD)
     * Calcula automáticamente el rango del día (00:00:00 a 23:59:59) en zona horaria de Lima
     * Usado para mostrar viajes de "Ayer" en el modal de la vista de monitoreo
     * @param driverId ID del conductor
     * @param fecha Fecha en formato "YYYY-MM-DD" (ej: "2025-12-10")
     * @return Respuesta con viajes simplificados del conductor
     */
    @GetMapping("/driver/viajes-simplificados-por-fecha")
    public ResponseEntity<DriverTripsSimplifiedResponse> obtenerViajesSimplificadosPorFecha(
            @RequestParam String driverId,
            @RequestParam String fecha) {
        long startTime = System.currentTimeMillis();
        log.info("🚗 [FleetDriverController] Obteniendo viajes simplificados para driver_id: {}, fecha: {}", driverId, fecha);
        
        DriverTripsSimplifiedResponse response = driverOrdersService.obtenerViajesSimplificadosPorFecha(driverId, fecha);
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("✅ [FleetDriverController] Viajes simplificados obtenidos: {} viajes - Tiempo total: {} ms ({:.2f} seg)", 
            response.getTrips() != null ? response.getTrips().size() : 0, totalTime, String.format("%.2f", totalTime / 1000.0));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 📋 VISTA: DetalleView
     * Registra un cierre de caja para un conductor en una fecha específica
     * @param request Datos del cierre (driverId, fecha, userId opcional, gastos, ingresos, etc.)
     * @return Cierre registrado
     */
    @PostMapping("/driver/registrar-cierre")
    public ResponseEntity<DriverClose> registrarCierre(
            @Valid @RequestBody DriverCloseRequest request) {
        log.info("💰 [FleetDriverController] Registrando cierre para driver_id: {}, fecha: {}, registrado por user_id: {}", 
            request.getDriverId(), request.getFecha(), request.getUserId());
        DriverClose driverClose = driverCloseService.registrarCierre(request);
        log.info("✅ [FleetDriverController] Cierre registrado exitosamente con ID: {}, creado por user_id: {}", 
            driverClose.getId(), driverClose.getUserId());
        return ResponseEntity.ok(driverClose);
    }

    /**
     * 📋 VISTA: DetalleView
     * Obtiene un cierre de caja existente por driver_id y fecha
     * @param driverId ID del conductor
     * @param fecha Fecha del cierre (formato: "2025-12-14")
     * @return Cierre encontrado o 404 si no existe
     */
    @GetMapping("/driver/cierre")
    public ResponseEntity<?> obtenerCierre(
            @RequestParam String driverId,
            @RequestParam String fecha) {
        log.info("🔍 [FleetDriverController] Buscando cierre para driver_id: {}, fecha: {}", driverId, fecha);
        Optional<DriverCloseResponse> cierreOpt = driverCloseService.obtenerCierrePorDriverIdYFecha(driverId, fecha);
        
        if (cierreOpt.isPresent()) {
            DriverCloseResponse response = cierreOpt.get();
            log.info("✅ [FleetDriverController] Cierre encontrado con ID: {}", response.getId());
            return ResponseEntity.ok(response);
        } else {
            log.info("ℹ️ [FleetDriverController] No se encontró cierre para driver_id: {} y fecha: {}", driverId, fecha);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Cierre no encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * 📋 VISTA: DetalleView
     * Actualiza un cierre de caja existente para un conductor en una fecha específica
     * @param request Datos del cierre actualizados (driverId, fecha, userId opcional, gastos, ingresos, etc.)
     * @return Cierre actualizado
     */
    @PutMapping("/driver/cierre")
    public ResponseEntity<DriverClose> actualizarCierre(
            @Valid @RequestBody DriverCloseRequest request) {
        log.info("🔄 [FleetDriverController] Actualizando cierre para driver_id: {}, fecha: {}, actualizado por user_id: {}", 
            request.getDriverId(), request.getFecha(), request.getUserId());
        try {
            DriverClose driverClose = driverCloseService.actualizarCierre(request);
            log.info("✅ [FleetDriverController] Cierre actualizado exitosamente con ID: {}, actualizado por user_id: {}", 
                driverClose.getId(), driverClose.getUserId());
            return ResponseEntity.ok(driverClose);
        } catch (RuntimeException e) {
            log.warn("⚠️ [FleetDriverController] No se encontró cierre para actualizar: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 🚗 VISTA: MonitoreoEnVivoView
     * Obtiene todos los conductores con status "in_order" y "free" con sus detalles
     * Incluye: avatar_url, balance, first_name, last_name, id, status, vehicle_number, viajes del día
     * El scheduler actualiza estos datos cada 5 minutos y los envía por WebSocket
     * @param page Número de página (default: 0, primera página)
     * @param limit Cantidad de conductores por página (default: 4)
     * @return Respuesta con lista de conductores en orden y sus detalles
     */
    @GetMapping("/drivers/in-order")
    public ResponseEntity<DriversInOrderResponse> obtenerConductoresEnOrden(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "4") Integer limit) {
        long startTime = System.currentTimeMillis();
        log.info("🚗 [FleetDriverController] Obteniendo conductores con status 'in_order' - page: {}, limit: {}", page, limit);
        
        DriversInOrderResponse response = fleetDriverService.obtenerConductoresEnOrden(page, limit);
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("✅ [FleetDriverController] Se encontraron {} conductores en orden (página {}) - Tiempo total: {} ms ({:.2f} seg)", 
            response.getTotal(), page, totalTime, String.format("%.2f", totalTime / 1000.0));
        return ResponseEntity.ok(response);
    }

    /**
     * 📋 ENDPOINT PRINCIPAL: Obtener o calcular turnos
     * Verifica si hay turnos en module_calculated_shifts para un driver y fecha.
     * Si no hay, calcula automáticamente y marca como es_manual = true.
     * @param driverId ID del conductor
     * @param fecha Fecha del turno (formato: "YYYY-MM-DD")
     * @return Mensaje de éxito indicando que los turnos están listos
     */
    @GetMapping("/driver/calcular-turnos")
    public ResponseEntity<Map<String, Object>> calcularTurnosManualmente(
            @RequestParam String driverId,
            @RequestParam String fecha) {
        log.info("📋 [FleetDriverController] Obteniendo o calculando turnos para driver_id: {}, fecha: {}", driverId, fecha);
        
        try {
            List<CalculatedShift> turnos = calculatedShiftService.obtenerOCalcularTurnos(driverId, fecha);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Turnos calculados exitosamente");
            response.put("driverId", driverId);
            response.put("fecha", fecha);
            response.put("cantidadTurnos", turnos.size());
            
            log.info("✅ [FleetDriverController] Se encontraron {} turno(s) para driver_id: {} y fecha: {}", 
                turnos.size(), driverId, fecha);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [FleetDriverController] Error obteniendo o calculando turnos para driver_id: {}, fecha: {}: {}", 
                driverId, fecha, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error al calcular turnos: " + e.getMessage());
            errorResponse.put("driverId", driverId);
            errorResponse.put("fecha", fecha);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 📋 VISTA: DetalleView
     * Obtiene las fechas únicas con sus tipos de turno para un conductor
     * Las fechas no se repiten y pueden tener uno o dos tipos de turno (diurno, nocturno)
     * @param driverId ID del conductor
     * @return Respuesta con fechas únicas y sus tipos de turno con sus IDs
     */
    @GetMapping("/driver/fechas-turnos/{driverId}")
    public ResponseEntity<FechasConTiposTurnoResponse> obtenerFechasConTiposTurno(
            @PathVariable String driverId) {
        log.info("📅 [FleetDriverController] Obteniendo fechas con tipos de turno para driver_id: {}", driverId);
        FechasConTiposTurnoResponse response = calculatedShiftService.obtenerFechasConTiposTurno(driverId);
        log.info("✅ [FleetDriverController] Se encontraron {} fechas únicas para driver_id: {}", 
                response.getFechas().size(), driverId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 💰 VISTA: Resumen de Pagos
     * Obtiene el resumen de pagos de todos los conductores con:
     * - driver_id
     * - avatar_url
     * - nombre
     * - telefono
     * - monto_total_pagar (suma de monto_total de turnos no pagados)
     * - cantidad_turnos
     * - lista de turnos con sus detalles
     * @param fecha Fecha para filtrar los turnos (formato: "YYYY-MM-DD", ejemplo: "2026-01-15")
     * @return Respuesta con lista de conductores y sus turnos de la fecha especificada
     */
    @GetMapping("/drivers/resumen-pagos")
    public ResponseEntity<DriverPaymentSummaryResponse> obtenerResumenPagos(
            @RequestParam String fecha) {
        log.info("💰 [FleetDriverController] Obteniendo resumen de pagos de todos los conductores para fecha: {}", fecha);
        DriverPaymentSummaryResponse response = calculatedShiftService.obtenerResumenPagos(fecha);
        log.info("✅ [FleetDriverController] Resumen de pagos obtenido para {} conductores en fecha {}", 
                response.getConductores() != null ? response.getConductores().size() : 0, fecha);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 💰 VISTA: Lista de Turnos Pagados
     * Obtiene todos los turnos que ya están pagados (pagado = true)
     * @param fecha Fecha opcional para filtrar los turnos pagados (formato: "YYYY-MM-DD", null para todos)
     * @return Respuesta con lista de turnos pagados y el total
     */
    @GetMapping("/drivers/turnos-pagados")
    public ResponseEntity<PaidShiftsResponse> obtenerTurnosPagados(
            @RequestParam(required = false) String fecha) {
        log.info("💰 [FleetDriverController] Obteniendo turnos pagados{}", 
            fecha != null ? " para fecha: " + fecha : "");
        PaidShiftsResponse response = calculatedShiftService.obtenerTurnosPagados(fecha);
        log.info("✅ [FleetDriverController] Se encontraron {} conductores con turnos pagados{}", 
                response.getTotalConductores(), fecha != null ? " para fecha: " + fecha : "");
        return ResponseEntity.ok(response);
    }

    /**
     * 📋 ENDPOINT: Lista de conductores simplificada
     * Obtiene una lista de todos los conductores con solo: nombre, telefono, driver_id y avatar_url
     * @return Lista de conductores con información básica
     */
    @GetMapping("/drivers")
    public ResponseEntity<DriverSimpleResponse> obtenerListaConductores(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String fecha) {
        
        // Si se proporciona nombre y fecha, buscar por nombre y filtrar por turnos manuales
        if (nombre != null && !nombre.trim().isEmpty() && fecha != null && !fecha.trim().isEmpty()) {
            log.info("🔍 [FleetDriverController] Buscando conductores por nombre: '{}', fecha: {}", nombre, fecha);
            DriverSimpleResponse response = fleetDriverService.buscarConductoresPorNombre(nombre, fecha);
            
            // Si hay un mensaje de error, devolverlo
            if (response.getMensaje() != null && !response.getMensaje().isEmpty()) {
                return ResponseEntity.badRequest().body(response);
            }
            
            return ResponseEntity.ok(response);
        }
        
        // Si no se proporciona nombre, devolver lista completa
        log.info("📋 [FleetDriverController] Obteniendo lista de conductores");
        DriverSimpleResponse response = fleetDriverService.obtenerListaConductoresSimplificada();
        return ResponseEntity.ok(response);
    }
}

