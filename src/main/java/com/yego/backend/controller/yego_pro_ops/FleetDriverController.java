package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.ContractorSuggestionsRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.DriverCloseRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.ContractorSuggestionsResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverCloseResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverSimpleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverPaymentSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.PaidShiftsResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.FechasConTiposTurnoResponse;
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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

    @GetMapping("/driver/viajes-completos")
    public DriverOrdersResponse obtenerViajesCompletos(
            @RequestParam String driverId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo);
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

    @PostMapping("/turnos/generar-dia-anterior")
    public ResponseEntity<Map<String, Object>> generarTurnosDiaAnterior() {
        log.info("[FleetDriverController] disparo manual batch día anterior (fire-and-forget)");
        CompletableFuture.runAsync(() -> {
            try {
                calculatedShiftService.procesarHorasTurnoDiaAnterior();
            } catch (Exception e) {
                log.error("[FleetDriverController] error batch manual día anterior: {}", e.getMessage(), e);
            }
        });
        return ResponseEntity.accepted().body(Map.of(
            "message", "Batch encolado, se procesará en segundo plano. Revisa logs o consulta el resumen de pagos."
        ));
    }

    @GetMapping("/driver/calcular-turnos")
    public CompletableFuture<Map<String, Object>> calcularTurnosManualmente(
            @RequestParam String driverId,
            @RequestParam String fecha) {
        return calculatedShiftService.calcularTurnosAsync(driverId, fecha)
            .handle((turnos, error) -> {
                if (error != null) {
                    Throwable causa = error instanceof CompletionException && error.getCause() != null
                        ? error.getCause() : error;
                    log.error("[FleetDriverController] error calcular-turnos driverId={} fecha={}: {}",
                        driverId, fecha, causa.getMessage(), causa);
                    if (causa instanceof IllegalArgumentException) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, causa.getMessage());
                    }
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error al calcular turnos: " + causa.getMessage());
                }
                return Map.<String, Object>of(
                    "message", "Turnos calculados exitosamente",
                    "driverId", driverId,
                    "fecha", fecha,
                    "cantidadTurnos", turnos.size()
                );
            });
    }

    @GetMapping("/driver/fechas-turnos/{driverId}")
    public FechasConTiposTurnoResponse obtenerFechasConTiposTurno(@PathVariable String driverId) {
        return calculatedShiftService.obtenerFechasConTiposTurno(driverId);
    }

    @GetMapping("/drivers/resumen-pagos")
    public DriverPaymentSummaryResponse obtenerResumenPagos(@RequestParam String fecha) {
        return calculatedShiftService.obtenerResumenPagos(fecha);
    }

    @GetMapping("/drivers/turnos-pagados")
    public PaidShiftsResponse obtenerTurnosPagados(@RequestParam(required = false) String fecha) {
        return calculatedShiftService.obtenerTurnosPagados(fecha);
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
}
