package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.MobileVehicleCard;
import com.yego.backend.entity.yego_pro_ops.api.response.MobileVehicleResponse;
import com.yego.backend.service.yego_pro_ops.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * API especial para la app móvil.
 * Flujo: buscar por placa (ligero) -> abrir detalle por yangoCarId (completo).
 * Todo desde BD local (rápido, sin Yango).
 */
@Slf4j
@RestController
@RequestMapping("/api/vehicles/mobile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MobileVehicleController {

    private final VehicleService vehicleService;

    /** Buscador ligero por placa (parcial). Devuelve lista de cards. */
    @GetMapping("/search")
    public List<MobileVehicleCard> buscar(@RequestParam String placa) {
        return vehicleService.buscarVehiculosMobile(placa);
    }

    /** Detalle completo del vehículo por yangoCarId. */
    @GetMapping("/{yangoCarId}")
    public MobileVehicleResponse detalle(@PathVariable String yangoCarId) {
        try {
            return vehicleService.obtenerVehiculoMobile(yangoCarId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
