package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.GrupoRequest;
import com.yego.backend.entity.yego_principal.api.response.GrupoResponse;
import com.yego.backend.service.yego_principal.GrupoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GrupoController {

    private final GrupoService grupoService;

    @GetMapping("/activos")
    public ResponseEntity<List<GrupoResponse>> obtenerActivos() {
        log.info("📋 [GrupoController] GET /api/grupos/activos - Obteniendo grupos activos");
        return ResponseEntity.ok(grupoService.obtenerActivos());
    }

    @PostMapping
    public ResponseEntity<GrupoResponse> crear(@Valid @RequestBody GrupoRequest request) {
        log.info("📋 [GrupoController] POST /api/grupos - Creando grupo: {}", request.getNombre());
        GrupoResponse response = grupoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GrupoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody GrupoRequest request) {
        log.info("📋 [GrupoController] PUT /api/grupos/{} - Actualizando grupo", id);
        return ResponseEntity.ok(grupoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id) {
        log.info("📋 [GrupoController] DELETE /api/grupos/{} - Eliminando grupo", id);
        grupoService.eliminar(id);
        return ResponseEntity.ok(Map.of("message", "Grupo eliminado correctamente"));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, String>> toggleActive(@PathVariable Long id) {
        log.info("📋 [GrupoController] PATCH /api/grupos/{}/toggle - Toggle estado", id);
        grupoService.toggleActive(id);
        return ResponseEntity.ok(Map.of("message", "Estado del grupo actualizado correctamente"));
    }
}

