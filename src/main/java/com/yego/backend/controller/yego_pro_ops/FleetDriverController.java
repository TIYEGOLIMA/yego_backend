package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.DriverCloseRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.DriverOrdersRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverKpiResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.FechasConTiposTurnoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.WorkRulesResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import com.yego.backend.service.yego_pro_ops.DriverCloseService;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    
    @GetMapping("/kpis")
    public ResponseEntity<DriverKpiResponse> obtenerKpis() {
        return ResponseEntity.ok(fleetDriverService.obtenerKpisActuales());
    }
    
    @PostMapping("/kpis/refresh")
    public ResponseEntity<DriverKpiResponse> refrescarKpis() {
        return ResponseEntity.ok(fleetDriverService.consultarConductores());
    }

    /**
     * Obtiene la lista completa de conductores usando la API de contractors de Yango
     * @param workRuleIds Lista opcional de IDs de reglas de trabajo para filtrar conductores
     * @return Lista de conductores con información detallada
     */
    @GetMapping("/drivers")
    public ResponseEntity<DriverListResponse> obtenerListaConductores(
            @RequestParam(required = false) List<String> work_rule_ids) {
        log.info("📋 [FleetDriverController] Obteniendo lista de conductores - work_rule_ids: {}", work_rule_ids);
        DriverListResponse response = fleetDriverService.obtenerListaConductores(work_rule_ids);
        log.info("✅ [FleetDriverController] Lista de conductores obtenida: {} conductores", 
            response.getContractors() != null ? response.getContractors().size() : 0);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/driver/orders")
    public ResponseEntity<DriverOrdersResponse> obtenerOrdenesDelDia(@Valid @RequestBody DriverOrdersRequest request) {
        log.info("📋 [FleetDriverController] Obteniendo órdenes para driver_id: {}", request.getDriverId());
        return ResponseEntity.ok(driverOrdersService.obtenerOrdenesDelDia(request));
    }
    
    /**
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
     * Obtiene TODOS los viajes (completos y cancelados) sin filtrar por status
     * Útil para cálculos de turnos donde se necesitan todos los viajes para determinar hora de inicio y fin
     * @param driverId ID del conductor
     * @param dateFrom Fecha inicial (formato: "2025-12-10T00:00:00-05:00")
     * @param dateTo Fecha final (formato: "2025-12-10T23:59:59-05:00")
     * @param cursor Cursor opcional para paginación (obtenido de la respuesta anterior)
     * @return Respuesta con lista de TODOS los viajes (sin filtrar por status)
     */
    @GetMapping("/driver/viajes-todos")
    public ResponseEntity<DriverOrdersResponse> obtenerTodosLosViajes(
            @RequestParam String driverId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String cursor) {
        log.info("🚗 [FleetDriverController] Obteniendo TODOS los viajes (sin filtro) para driver_id: {}, desde: {}, hasta: {}, cursor: {}", 
            driverId, dateFrom, dateTo, cursor != null ? "presente" : "no presente");
        
        DriverOrdersResponse response = driverOrdersService.obtenerTodosLosViajes(driverId, dateFrom, dateTo, cursor);
        log.info("✅ [FleetDriverController] Total de viajes devueltos: {}, hasMore: {}", 
            response.getOrders().size(), response.getHasMore());
        
        return ResponseEntity.ok(response);
    }

    /**
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
     * Obtiene un cierre de caja existente por driver_id y fecha
     * @param driverId ID del conductor
     * @param fecha Fecha del cierre (formato: "2025-12-14")
     * @return Cierre encontrado o 404 si no existe
     */
    @GetMapping("/driver/cierre")
    public ResponseEntity<DriverCloseResponse> obtenerCierre(
            @RequestParam String driverId,
            @RequestParam String fecha) {
        log.info("🔍 [FleetDriverController] Buscando cierre para driver_id: {}, fecha: {}", driverId, fecha);
        return driverCloseService.obtenerCierrePorDriverIdYFecha(driverId, fecha)
            .map(response -> {
                log.info("✅ [FleetDriverController] Cierre encontrado con ID: {}", response.getId());
                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                log.info("ℹ️ [FleetDriverController] No se encontró cierre para driver_id: {} y fecha: {}", driverId, fecha);
                return ResponseEntity.notFound().build();
            });
    }

    /**
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
     * Obtiene la lista de reglas de trabajo (work rules) desde la API de Yango
     * @return Lista de reglas de trabajo con información básica
     */
    @GetMapping("/work-rules")
    public ResponseEntity<WorkRulesResponse> obtenerReglasTrabajo() {
        log.info("📋 [FleetDriverController] Obteniendo reglas de trabajo");
        WorkRulesResponse response = fleetDriverService.obtenerReglasTrabajo();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene todos los conductores con status "in_order" y sus detalles
     * Incluye: avatar_url, balance, first_name, last_name, id, status, route, vehicle_number
     * El scheduler actualiza estos datos cada 10 segundos y los envía por WebSocket
     * @return Respuesta con lista de conductores en orden y sus detalles
     */
    @GetMapping("/drivers/in-order")
    public ResponseEntity<DriversInOrderResponse> obtenerConductoresEnOrden() {
        log.info("🚗 [FleetDriverController] Obteniendo conductores con status 'in_order'");
        DriversInOrderResponse response = fleetDriverService.obtenerConductoresEnOrden();
        log.info("✅ [FleetDriverController] Se encontraron {} conductores en orden", response.getTotal());
        return ResponseEntity.ok(response);
    }

    /**
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
}

