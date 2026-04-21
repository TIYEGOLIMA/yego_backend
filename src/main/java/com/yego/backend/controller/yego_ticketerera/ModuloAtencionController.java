package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloUsuarioResponse;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticketera/modulo-atencion")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ModuloAtencionController {

    private final ModuloAtencionService moduloAtencionService;

    @GetMapping("/activos")
    public ResponseEntity<List<ModuloAtencionResponse>> obtenerTodosLosModulosActivos() {
        return ResponseEntity.ok(moduloAtencionService.obtenerTodosLosModulosActivosResponse());
    }

    @GetMapping("/usuario/{userId}")
    public ResponseEntity<ModuloUsuarioResponse> verificarModuloOListarDisponibles(
            @PathVariable Long userId,
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(moduloAtencionService.verificarModuloOListarDisponibles(userId, sedeId));
    }
}
