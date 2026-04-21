package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearDispositivoRequest;
import com.yego.backend.entity.yego_ticketerera.api.request.DispositivoAuthRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.DispositivoResponse;
import com.yego.backend.service.yego_ticketerera.DispositivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ticketera/dispositivos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DispositivoController {

    private final DispositivoService dispositivoService;

    @GetMapping
    public ResponseEntity<List<DispositivoResponse>> listar() {
        return ResponseEntity.ok(dispositivoService.listarDispositivos());
    }

    @PostMapping
    public ResponseEntity<DispositivoResponse> crear(@Valid @RequestBody CrearDispositivoRequest request) {
        return ResponseEntity.ok(dispositivoService.crearDispositivo(request));
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

    @PostMapping("/{id}/regenerar-token")
    public ResponseEntity<DispositivoResponse> regenerarToken(@PathVariable Long id) {
        return ResponseEntity.ok(dispositivoService.regenerarTokenAcceso(id));
    }

    @PatchMapping("/{id}/modulo")
    public ResponseEntity<DispositivoResponse> asignarModulo(@PathVariable Long id,
                                                             @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(dispositivoService.asignarModulo(id, body.get("moduleId")));
    }

    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> autenticar(@Valid @RequestBody DispositivoAuthRequest request) {
        return ResponseEntity.ok(dispositivoService.autenticarDispositivo(request.getAccessToken()));
    }
}
