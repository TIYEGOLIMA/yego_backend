package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloUsuarioResponse;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de módulos de atención del sistema YEGO Ticketerera
 */
@RestController
@RequestMapping("/api/ticketera/modulo-atencion")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ModuloAtencionController {
    
    private final ModuloAtencionService moduloAtencionService;

    @GetMapping("/activos")
    public ResponseEntity<List<ModuloAtencionResponse>> obtenerTodosLosModulosActivos() {
        return ResponseEntity.ok(moduloAtencionService.obtenerTodosLosModulosActivosResponse());
    }
    
    @GetMapping("/usuario/{userId}")
    public ResponseEntity<ModuloUsuarioResponse> verificarModuloOListarDisponibles(@PathVariable Long userId) {
        log.info("[ModuloAtencion] Verificar módulo o listar disponibles para usuario {}", userId);
        ModuloUsuarioResponse response = moduloAtencionService.verificarModuloOListarDisponibles(userId);
        log.info("[ModuloAtencion] Usuario {} tiene módulo asignado: {}", userId, response.getTieneModuloAsignado());
        return ResponseEntity.ok(response);
    }
}
