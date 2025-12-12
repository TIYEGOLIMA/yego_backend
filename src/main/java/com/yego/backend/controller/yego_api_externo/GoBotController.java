package com.yego.backend.controller.yego_api_externo;

import com.yego.backend.entity.yego_api_externo.api.request.PPendientesRequest;
import com.yego.backend.entity.yego_api_externo.api.response.PPendientesResponse;
import com.yego.backend.service.yego_garantizado.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para APIs externas - GoBot
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@RestController
@RequestMapping("/api/GoBot")
public class GoBotController {

    private final DriverService driverService;

    public GoBotController(DriverService driverService) {
        this.driverService = driverService;
    }
    
    /**
     * Endpoint para GoBot - Obtiene información de pagos pendientes
     * @param request Request con el teléfono o licencia del conductor
     * @return Respuesta con información de pagos pendientes
     */
    @PostMapping("/PPendientes")
    public ResponseEntity<PPendientesResponse> obtenerPendientes(@Valid @RequestBody PPendientesRequest request) {
        PPendientesResponse response = driverService.obtenerPendientes(request.getTelefono());
        return ResponseEntity.ok(response);
    }
}

