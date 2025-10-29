package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.service.yego_ticketerera.DriverConsultaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la gestión de conductores del sistema YEGO Ticketerera
 */
@RestController
@RequestMapping("/api/ticketera")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DriverController {

    private final DriverConsultaService driverConsultaService;
    
    @GetMapping("/buscar/telefono/{phoneDigits}")
     public ResponseEntity<Map<String, Object>> buscarPorTelefonoFrontend(@PathVariable String phoneDigits) {
        return driverConsultaService.buscarConductorConRespuestaCompleta(phoneDigits);
    }
    
    @PostMapping("/drivers/registrar")
    public ResponseEntity<Map<String, Object>> registrarConductorManual(@RequestBody Map<String, String> datos) {
        return driverConsultaService.registrarConductorManualConRespuesta(datos);
    }
    
    @PostMapping("/drivers/registrar-dni")
    public ResponseEntity<Map<String, Object>> registrarConductorPorDni(@RequestBody Map<String, String> datos) {
        return driverConsultaService.registrarConductorPorDniConRespuesta(datos);
    }
    
    @GetMapping("/drivers/cache/clear")
    public ResponseEntity<Map<String, Object>> limpiarCache() {
        return driverConsultaService.limpiarCacheConRespuesta();
    }
}
