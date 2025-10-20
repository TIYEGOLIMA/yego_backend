package com.yego.backend.controller.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.DriverValidationResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaDisponibleResponse;
import com.yego.backend.service.yego_garantizado.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para operaciones relacionadas con conductores
 * Maneja la validación de licencias y obtención de flotas
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@RestController
@RequestMapping("/api/drivers")
public class DriverGarantizadoController {

    private final DriverService driverService;

    public DriverGarantizadoController(DriverService driverService) {
        this.driverService = driverService;
    }
    
    /**
     * Valida una licencia de conductor
     * @param licenseNumber Número de licencia a validar
     * @return Respuesta con información de validación
     */
    @GetMapping("/validar/{licenseNumber}")
    public ResponseEntity<DriverValidationResponse> validarLicencia(@PathVariable String licenseNumber) {
        DriverValidationResponse response = driverService.validarLicencia(licenseNumber);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene información del conductor con sus flotas disponibles
     * @param licenseNumber Número de licencia del conductor
     * @return Respuesta con conductor y flotas
     */
    @GetMapping("/flotas/{licenseNumber}")
    public ResponseEntity<FlotaDisponibleResponse> obtenerConductorConFlotas(@PathVariable String licenseNumber) {
        FlotaDisponibleResponse response = driverService.obtenerConductorConFlotas(licenseNumber);
        return ResponseEntity.ok(response);
    }
}
