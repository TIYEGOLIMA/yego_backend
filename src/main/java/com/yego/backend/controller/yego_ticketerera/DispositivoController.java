package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearDispositivoRequest;
import com.yego.backend.entity.yego_ticketerera.api.request.DispositivoAuthRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.DispositivoResponse;
import com.yego.backend.service.yego_ticketerera.DispositivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ticketera/dispositivos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DispositivoController {

    private final DispositivoService dispositivoService;

    @GetMapping
    public ResponseEntity<List<DispositivoResponse>> listar() {
        return ResponseEntity.ok(dispositivoService.listarDispositivos());
    }

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<DispositivoResponse>> listarPorSede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(dispositivoService.listarDispositivosPorSede(sedeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DispositivoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(dispositivoService.obtenerDispositivo(id));
    }

    @PostMapping
    public ResponseEntity<DispositivoResponse> crear(@Valid @RequestBody CrearDispositivoRequest request) {
        DispositivoResponse response = dispositivoService.crearDispositivo(request);
        log.info("[DispositivoController] Dispositivo creado: {}", response.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DispositivoResponse> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody CrearDispositivoRequest request) {
        return ResponseEntity.ok(dispositivoService.actualizarDispositivo(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        dispositivoService.desactivarDispositivo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> autenticar(@Valid @RequestBody DispositivoAuthRequest request) {
        log.info("[DispositivoController] Intento de autenticación de dispositivo");
        Map<String, Object> result = dispositivoService.autenticarDispositivo(request.getAccessToken());
        return ResponseEntity.ok(result);
    }
}
