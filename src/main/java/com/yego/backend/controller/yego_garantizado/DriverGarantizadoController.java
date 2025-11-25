package com.yego.backend.controller.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.DriverValidationResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaDisponibleResponse;
import com.yego.backend.entity.yego_api_externo.api.request.PPendientesRequest;
import com.yego.backend.entity.yego_api_externo.api.response.PPendientesResponse;
import com.yego.backend.service.yego_garantizado.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    
    /**
     * Endpoint para GoBot - Obtiene información de pagos pendientes
     * @param request Request con el teléfono del conductor
     * @return Respuesta con información de pagos pendientes
     */
    @PostMapping("/GoBot/PPendientes")
    public ResponseEntity<PPendientesResponse> obtenerPendientes(@Valid @RequestBody PPendientesRequest request) {
        PPendientesResponse response = driverService.obtenerPendientes(request.getTelefono());
        return ResponseEntity.ok(response);
    }
}
