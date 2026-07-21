package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.FleetVehicleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.VehicleTraceEvent;
import com.yego.backend.entity.yego_pro_ops.api.response.VehicleResponse;
import com.yego.backend.entity.yego_pro_ops.entities.*;
import com.yego.backend.config.yego_pro_ops.YegoProOpsProperties;
import com.yego.backend.service.yego_pro_ops.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleService vehicleService;
    private final YegoProOpsProperties proOpsProperties;

    // ── Yango Fleet API proxy ──

    @GetMapping("/yango-fleet")
    public Map<String, Object> listarVehiculos(
            @RequestParam(required = false) String parkId,
            @RequestParam(required = false) String cursor) {
        return vehicleService.listarVehiculosYango(resolveParkId(parkId), cursor);
    }

    @GetMapping("/yango-fleet/all")
    @SuppressWarnings("unchecked")
    public Map<String, Object> listarTodos(@RequestParam(required = false) String parkId) {
        String effectiveParkId = resolveParkId(parkId);
        List<Map<String, Object>> allCars = new ArrayList<>();
        String cursor = null;
        int total = 0;

        do {
            Map<String, Object> page = vehicleService.listarVehiculosYango(effectiveParkId, cursor);
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

    // ── Flota cacheada (segmentación) ──

    @GetMapping("/fleet")
    public ResponseEntity<Map<String, Object>> listarFlotaGuardada(@RequestParam(required = false) UUID segmentId) {
        List<FleetVehicleResponse> cars = vehicleService.listarVehiculosGuardados(segmentId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("total", cars.size(), "cars", cars));
    }

    @PostMapping("/fleet/sync")
    public Map<String, Object> sincronizar(@RequestParam(required = false) UUID segmentId) {
        int procesados = (segmentId != null)
                ? vehicleService.sincronizarFlota(segmentId)
                : vehicleService.sincronizarTodas();
        return Map.of("procesados", procesados);
    }

    @GetMapping("/by-placa/{placa}")
    public VehicleResponse detallePorPlaca(@PathVariable String placa) {
        return vehicleService.obtenerDetallePorPlaca(placa);
    }

    @GetMapping("/{yangoCarId}/history")
    public List<VehicleTraceEvent> trazabilidad(@PathVariable String yangoCarId) {
        return vehicleService.obtenerTrazabilidad(yangoCarId);
    }

    private Long usuarioId(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        try { return Long.parseLong(auth.getName()); } catch (NumberFormatException e) { return null; }
    }

    @GetMapping("/{carId}/details")
    public VehicleResponse detalles(@PathVariable String carId,
                                     @RequestParam(required = false) String parkId) {
        return vehicleService.obtenerDetalleVehiculo(carId, resolveParkId(parkId));
    }

    @GetMapping("/{carId}/qc-history")
    public Map<String, Object> historialQc(@PathVariable String carId,
                                            @RequestParam(required = false) String parkId) {
        return vehicleService.obtenerHistorialQc(carId, resolveParkId(parkId));
    }

    private String resolveParkId(String parkId) {
        return parkId == null || parkId.isBlank() ? proOpsProperties.getParkId() : parkId;
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

    @PostMapping(value = "/{yangoCarId}/documents/upload", consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse.DocumentInfo agregarDocumentoConArchivo(
            @PathVariable String yangoCarId,
            @RequestParam String tipo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVigente,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        return vehicleService.agregarDocumentoConArchivo(yangoCarId, tipo, nombre, fechaVigente, file, usuarioId(auth));
    }

    @PostMapping(value = "/{yangoCarId}/maintenance/upload", consumes = {"multipart/form-data"})
    public Map<String, String> subirArchivoMantenimiento(
            @PathVariable String yangoCarId,
            @RequestParam("file") MultipartFile file) {
        String url = vehicleService.subirArchivoMantenimiento(yangoCarId, file);
        return Map.of("url", url);
    }

    @DeleteMapping("/documents/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarDocumento(@PathVariable Long docId, Authentication auth) {
        vehicleService.eliminarDocumento(docId, usuarioId(auth));
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
