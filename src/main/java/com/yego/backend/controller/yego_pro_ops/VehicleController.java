package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.VehicleResponse;
import com.yego.backend.entity.yego_pro_ops.entities.*;
import com.yego.backend.service.yego_pro_ops.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleService vehicleService;

    // ── Yango Fleet API proxy ──

    @GetMapping("/yango-fleet")
    public Map<String, Object> listarVehiculos(
            @RequestParam(defaultValue = "64085dd85e124e2c808806f70d527ea8") String parkId,
            @RequestParam(required = false) String cursor) {
        return vehicleService.listarVehiculosYango(parkId, cursor);
    }

    @GetMapping("/yango-fleet/all")
    @SuppressWarnings("unchecked")
    public Map<String, Object> listarTodos(@RequestParam(defaultValue = "64085dd85e124e2c808806f70d527ea8") String parkId) {
        List<Map<String, Object>> allCars = new ArrayList<>();
        String cursor = null;
        int total = 0;

        do {
            Map<String, Object> page = vehicleService.listarVehiculosYango(parkId, cursor);
            Object carsObj = page.get("cars");
            if (carsObj instanceof List) {
                allCars.addAll((List<Map<String, Object>>) carsObj);
            }
            cursor = (String) page.get("cursor");
            Object totalObj = page.get("total");
            if (totalObj instanceof Number) {
                total = ((Number) totalObj).intValue();
            }
        } while (cursor != null && !cursor.isBlank());

        return Map.of("total", total, "cars", allCars);
    }

    @GetMapping("/{carId}/details")
    public VehicleResponse detalles(@PathVariable String carId,
                                     @RequestParam(defaultValue = "64085dd85e124e2c808806f70d527ea8") String parkId) {
        return vehicleService.obtenerDetalleVehiculo(carId, parkId);
    }

    @GetMapping("/{carId}/qc-history")
    public Map<String, Object> historialQc(@PathVariable String carId,
                                            @RequestParam(defaultValue = "64085dd85e124e2c808806f70d527ea8") String parkId) {
        return vehicleService.obtenerHistorialQc(carId, parkId);
    }

    // ── Custom CRUD (local DB) ──

    @GetMapping("/{yangoCarId}/documents")
    public List<VehicleResponse.DocumentInfo> documentos(@PathVariable String yangoCarId) {
        return vehicleService.obtenerDocumentos(yangoCarId);
    }

    @PostMapping("/{yangoCarId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse.DocumentInfo agregarDocumento(@PathVariable String yangoCarId, @RequestBody VehicleDocument doc) {
        return vehicleService.agregarDocumento(yangoCarId, doc);
    }

    @DeleteMapping("/documents/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarDocumento(@PathVariable Long docId) {
        vehicleService.eliminarDocumento(docId);
    }

    @GetMapping("/{yangoCarId}/maintenance")
    public List<VehicleResponse.MaintenanceInfo> mantenimientos(@PathVariable String yangoCarId) {
        return vehicleService.obtenerMantenimientos(yangoCarId);
    }

    @PostMapping("/{yangoCarId}/maintenance")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse.MaintenanceInfo agregarMantenimiento(@PathVariable String yangoCarId, @RequestBody VehicleMaintenance mant) {
        return vehicleService.agregarMantenimiento(yangoCarId, mant);
    }

    @DeleteMapping("/maintenance/{mantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarMantenimiento(@PathVariable Long mantId) {
        vehicleService.eliminarMantenimiento(mantId);
    }

    @GetMapping("/{yangoCarId}/mileage")
    public List<VehicleResponse.MileageInfo> kilometraje(@PathVariable String yangoCarId) {
        return vehicleService.obtenerKilometraje(yangoCarId);
    }

    @PostMapping("/{yangoCarId}/mileage")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse.MileageInfo agregarKilometraje(@PathVariable String yangoCarId, @RequestBody VehicleMileage km) {
        return vehicleService.agregarKilometraje(yangoCarId, km);
    }

    @GetMapping("/{yangoCarId}/incidents")
    public List<VehicleResponse.IncidentInfo> siniestros(@PathVariable String yangoCarId) {
        return vehicleService.obtenerSiniestros(yangoCarId);
    }

    @PostMapping("/{yangoCarId}/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse.IncidentInfo agregarSiniestro(@PathVariable String yangoCarId, @RequestBody VehicleIncident inc) {
        return vehicleService.agregarSiniestro(yangoCarId, inc);
    }

    @DeleteMapping("/incidents/{incId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarSiniestro(@PathVariable Long incId) {
        vehicleService.eliminarSiniestro(incId);
    }
}
