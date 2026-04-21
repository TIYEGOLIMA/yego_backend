package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearModuloAtencionRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloUsuarioResponse;
import com.yego.backend.service.yego_ticketerera.ModuloAtencionService;
import jakarta.validation.Valid;
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

    @GetMapping("/usuario/{userId}")
    public ResponseEntity<ModuloUsuarioResponse> verificarModuloOListarDisponibles(
            @PathVariable Long userId,
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(moduloAtencionService.verificarModuloOListarDisponibles(userId, sedeId));
    }

    @GetMapping
    public ResponseEntity<List<ModuloAtencionResponse>> listarTodos(
            @RequestParam(required = false) Long sedeId) {
        return ResponseEntity.ok(moduloAtencionService.listarTodos(sedeId));
    }

    @PostMapping
    public ResponseEntity<ModuloAtencionResponse> crear(@Valid @RequestBody CrearModuloAtencionRequest request) {
        return ResponseEntity.ok(moduloAtencionService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModuloAtencionResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CrearModuloAtencionRequest request) {
        return ResponseEntity.ok(moduloAtencionService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        moduloAtencionService.cambiarEstadoModulo(id, activo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        moduloAtencionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
