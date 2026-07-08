package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.VehicleMobileResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.VehicleResponse;
import com.yego.backend.service.yego_pro_ops.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles/mobile")
@RequiredArgsConstructor
public class VehicleMobileController {

    private static final String DEFAULT_PARK_ID = "64085dd85e124e2c808806f70d527ea8";

    private final VehicleService vehicleService;

    @GetMapping("/search")
    public List<VehicleMobileResponse> search(@RequestParam String placa) {
        String q = placa == null ? "" : placa.trim().toUpperCase();
        if (q.isEmpty()) return List.of();

        List<VehicleMobileResponse> out = new ArrayList<>();
        String cursor = null;
        do {
            Map<String, Object> page = vehicleService.listarVehiculosYango(DEFAULT_PARK_ID, cursor);
            Object carsObj = page.get("cars");
            if (carsObj instanceof List<?> cars) {
                for (Object item : cars) {
                    if (item instanceof Map<?, ?> car) {
                        VehicleMobileResponse mapped = mapCar(car);
                        if (mapped.getPlaca() != null && mapped.getPlaca().toUpperCase().contains(q)) {
                            out.add(mapped);
                        }
                    }
                }
            }
            Object next = page.get("cursor");
            cursor = next instanceof String s && !s.isBlank() ? s : null;
        } while (cursor != null && out.size() < 20);

        return out;
    }

    @GetMapping("/{yangoCarId}")
    public Map<String, Object> detail(@PathVariable String yangoCarId) {
        VehicleResponse detail = vehicleService.obtenerDetalleVehiculo(yangoCarId, DEFAULT_PARK_ID);
        if (detail == null) {
            return Map.of();
        }
        Map<String, Object> general = new LinkedHashMap<>();
        general.put("yangoCarId", detail.getId());
        general.put("placa", value(detail.getNumber()));
        general.put("marca", value(detail.getBrand()));
        general.put("modelo", value(detail.getModel()));
        general.put("anio", detail.getYear() != null ? detail.getYear() : 0);
        general.put("color", value(detail.getColorName()));
        general.put("vin", value(detail.getVin()));
        general.put("estado", detail.getStatus() != null ? value(detail.getStatus().getName()) : "");
        general.put("flota", "Yego Pro");
        general.put("categorias", detail.getCategories() != null ? detail.getCategories() : List.of());
        general.put("amenities", detail.getAmenities() != null ? detail.getAmenities() : List.of());
        general.put("rental", Boolean.TRUE.equals(detail.getRental()));
        general.put("kilometraje", detail.getMileage() != null ? detail.getMileage() : 0);

        Map<String, Object> gastos = new LinkedHashMap<>();
        gastos.put("total", BigDecimal.ZERO);
        gastos.put("preventivo", BigDecimal.ZERO);
        gastos.put("correctivo", BigDecimal.ZERO);
        gastos.put("siniestros", BigDecimal.ZERO);
        gastos.put("pctPreventivo", 0);
        gastos.put("pctCorrectivo", 0);
        gastos.put("pctSiniestros", 0);
        gastos.put("porMes", List.of());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("general", general);
        response.put("imagen", detail.getFotoUrl());
        response.put("documentos", detail.getDocuments() != null ? detail.getDocuments() : List.of());
        response.put("mantenimiento", detail.getMaintenance() != null ? detail.getMaintenance() : List.of());
        response.put("kilometraje", detail.getMileageHistory() != null ? detail.getMileageHistory() : List.of());
        response.put("siniestros", detail.getIncidents() != null ? detail.getIncidents() : List.of());
        response.put("gastos", gastos);
        response.put("trazabilidad", List.of());
        return response;
    }

    private VehicleMobileResponse mapCar(Map<?, ?> car) {
        return VehicleMobileResponse.builder()
                .yangoCarId(text(car, "id", "car_id"))
                .placa(text(car, "number", "license_number", "plate"))
                .marca(text(car, "brand"))
                .modelo(text(car, "model"))
                .anio(number(car, "year"))
                .estado(status(car.get("status")))
                .flota("Yego Pro")
                .imagen(null)
                .build();
    }

    private String text(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) return String.valueOf(value);
        }
        return null;
    }

    private Integer number(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof Number n ? n.intValue() : null;
    }

    private String status(Object value) {
        if (value instanceof Map<?, ?> map) {
            Object name = map.get("name");
            Object id = map.get("id");
            return name != null ? String.valueOf(name) : id != null ? String.valueOf(id) : "";
        }
        return value != null ? String.valueOf(value) : "";
    }

    private String value(String value) {
        return value != null ? value : "";
    }
}
