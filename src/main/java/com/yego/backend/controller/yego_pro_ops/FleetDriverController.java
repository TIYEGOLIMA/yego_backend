package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.ContractorSuggestionsRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.DriverCloseRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.ContractorSuggestionsResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverSimpleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.BillingConfigResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.FacturacionSemanal;
import com.yego.backend.service.yego_pro_ops.DriverCloseService;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.FacturacionSemanalService;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pro-ops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FleetDriverController {

    private static final DateTimeFormatter YANGO_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final FleetDriverService fleetDriverService;
    private final DriverOrdersService driverOrdersService;
    private final DriverCloseService driverCloseService;
    private final FacturacionSemanalService facturacionSemanalService;

    @GetMapping("/driver/viajes-completos")
    public DriverOrdersResponse obtenerViajesCompletos(
            @RequestParam String driverId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo);
    }

    @GetMapping("/yango-trips")
    public Map<String, Object> obtenerSoloViajesYangoPorFecha(
            @RequestParam(name = "driver_id") String driverId,
            @RequestParam String fecha) {
        LocalDate date = LocalDate.parse(fecha);
        String desde = date.atStartOfDay().format(YANGO_DATETIME_FORMATTER) + "-05:00";
        String hasta = date.atTime(23, 59, 59).format(YANGO_DATETIME_FORMATTER) + "-05:00";
        DriverOrdersResponse response = driverOrdersService.obtenerViajesCompletos(driverId, desde, hasta);
        if (response == null || response.getOrders() == null) {
            return Map.of("cantidad_viajes", 0);
        }
        return Map.of("cantidad_viajes", response.getOrders().size());
    }

    @GetMapping("/driver/viajes-simplificados-por-fecha")
    public DriverTripsSimplifiedResponse obtenerViajesSimplificadosPorFecha(
            @RequestParam String driverId,
            @RequestParam String fecha) {
        return driverOrdersService.obtenerViajesSimplificadosPorFecha(driverId, fecha);
    }

    @PostMapping("/driver/registrar-cierre")
    public DriverClose registrarCierre(@Valid @RequestBody DriverCloseRequest request) {
        log.info("[FleetDriverController] registrar cierre driverId={} fecha={} userId={}",
            request.getDriverId(), request.getFecha(), request.getUserId());
        return driverCloseService.registrarCierre(request);
    }

    @GetMapping("/driver/cierre")
    public DriverCloseResponse obtenerCierre(
            @RequestParam String driverId,
            @RequestParam String fecha) {
        return driverCloseService.obtenerCierrePorDriverIdYFecha(driverId, fecha)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cierre no encontrado"));
    }

    @GetMapping("/driver/cierre/session/{sessionId}")
    public DriverCloseResponse obtenerCierrePorSession(@PathVariable java.util.UUID sessionId) {
        return driverCloseService.obtenerCierrePorSessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cierre no encontrado"));
    }

    @GetMapping("/driver/cierres/rango")
    public List<DriverCloseResponse> obtenerCierresPorRango(
            @RequestParam String driverId,
            @RequestParam String desde,
            @RequestParam String hasta) {
        return driverCloseService.obtenerCierresPorRango(driverId, desde, hasta);
    }

    @PutMapping("/driver/cierre")
    public DriverClose actualizarCierre(@Valid @RequestBody DriverCloseRequest request) {
        log.info("[FleetDriverController] actualizar cierre driverId={} fecha={} userId={}",
            request.getDriverId(), request.getFecha(), request.getUserId());
        try {
            return driverCloseService.actualizarCierre(request);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/drivers/in-order")
    public DriversInOrderResponse obtenerConductoresEnOrden(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "4") Integer limit) {
        return fleetDriverService.obtenerConductoresEnOrden(page, limit);
    }

    @GetMapping("/drivers")
    public DriverSimpleResponse obtenerListaConductores() {
        return fleetDriverService.obtenerListaConductoresSimplificada();
    }

    @PostMapping("/parks/{parkId}/contractor")
    public ResponseEntity<ContractorSuggestionsResponse> getContractorSuggestions(
            @PathVariable String parkId,
            @Valid @RequestBody ContractorSuggestionsRequest request) {
        return ResponseEntity.ok(fleetDriverService.getContractorSuggestions(parkId, request.getTelefono()));
    }

    @PostMapping("/drivers/facturacion-semanal")
    public FacturacionSemanal registrarFacturacionSemanal(@Valid @RequestBody FacturacionSemanal facturacion) {
        log.info("[FleetDriverController] registrar facturación semanal driverId={} semana={}/{} userId={}",
            facturacion.getDriverId(), facturacion.getFechaInicio(), facturacion.getFechaFin(), facturacion.getUserId());
        return facturacionSemanalService.registrarFacturacionSemanal(facturacion);
    }

    @GetMapping("/drivers/facturacion-semanal/historial")
    public List<FacturacionSemanal> obtenerHistorialFacturacion(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        return facturacionSemanalService.obtenerHistorialFacturacion(fechaInicio, fechaFin);
    }

    @GetMapping("/config/billing")
    public BillingConfigResponse obtenerConfiguracionBilling() {
        return facturacionSemanalService.obtenerConfiguracionBilling();
    }

    @PutMapping("/config/billing")
    public BillingConfigResponse guardarConfiguracionBilling(
            @Valid @RequestBody BillingConfigResponse config,
            @RequestParam Long userId) {
        return facturacionSemanalService.guardarConfiguracionBilling(config, userId);
    }
}
