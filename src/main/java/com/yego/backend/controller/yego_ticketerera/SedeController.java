package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearSedeRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.SedeResponse;
import com.yego.backend.service.yego_ticketerera.SedeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticketera/sedes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SedeController {

    private final SedeService sedeService;

    @GetMapping
    public ResponseEntity<List<SedeResponse>> listar() {
        return ResponseEntity.ok(sedeService.listarSedes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SedeResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(sedeService.obtenerSede(id));
    }

    @PostMapping
    public ResponseEntity<SedeResponse> crear(@Valid @RequestBody CrearSedeRequest request) {
        SedeResponse response = sedeService.crearSede(request);
        log.info("[SedeController] Sede creada: {}", response.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SedeResponse> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody CrearSedeRequest request) {
        return ResponseEntity.ok(sedeService.actualizarSede(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        sedeService.desactivarSede(id);
        return ResponseEntity.noContent().build();
    }
}
