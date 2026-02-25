package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.ModuleRequest;
import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import com.yego.backend.service.yego_principal.ModuleService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@Slf4j
public class ModuleController {

    private final ModuleService moduleService;

    /**
     * Ejecuta una acción y devuelve ResponseEntity. Maneja EntityNotFoundException (404)
     * y el resto de excepciones (500) con mensaje en el body.
     */
    private ResponseEntity<?> handle(Supplier<ResponseEntity<?>> action, String notFoundLog, String errorLog) {
        try {
            return action.get();
        } catch (EntityNotFoundException e) {
            log.warn("⚠️ [ModuleController] {}", notFoundLog, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [ModuleController] {}", errorLog, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    private ResponseEntity<List<ModuleResponse>> handleList(
            Supplier<List<ModuleResponse>> supplier, String requestLog, String successLog, String errorLog) {
        log.info("📋 [ModuleController] {}", requestLog);
        try {
            List<ModuleResponse> modulos = supplier.get();
            log.info("✅ [ModuleController] {}", successLog, modulos.size());
            return ResponseEntity.ok(modulos);
        } catch (Exception e) {
            log.error("❌ [ModuleController] {}", errorLog, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ModuleResponse>> obtenerTodos() {
        return handleList(
                moduleService::obtenerTodos,
                "Recibida solicitud para obtener todos los módulos",
                "Devueltos {} módulos",
                "Error obteniendo módulos: {}"
        );
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ModuleResponse>> obtenerActivos() {
        return handleList(
                moduleService::obtenerActivos,
                "Recibida solicitud para obtener módulos activos",
                "Devueltos {} módulos activos",
                "Error obteniendo módulos activos: {}"
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        log.info("📋 [ModuleController] Recibida solicitud para obtener módulo por ID: {}", id);
        return handle(
                () -> {
                    ModuleResponse modulo = moduleService.obtenerPorId(id);
                    log.info("✅ [ModuleController] Devuelto módulo con ID: {}", id);
                    return ResponseEntity.ok(modulo);
                },
                "Módulo no encontrado con ID " + id + ": {}",
                "Error obteniendo módulo por ID " + id + ": {}"
        );
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody ModuleRequest request) {
        log.info("📋 [ModuleController] Recibida solicitud para crear módulo: {}", request.getNombre());
        return handle(
                () -> {
                    ModuleResponse nuevoModulo = moduleService.crear(request);
                    log.info("✅ [ModuleController] Módulo creado con ID: {}", nuevoModulo.getId());
                    return ResponseEntity.status(HttpStatus.CREATED).body(nuevoModulo);
                },
                "Recurso no encontrado al crear: {}",
                "Error creando módulo: {}"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        log.info("📋 [ModuleController] Recibida solicitud para actualizar módulo con ID: {}", id);
        return handle(
                () -> {
                    ModuleResponse moduloActualizado = moduleService.actualizar(id, request);
                    log.info("✅ [ModuleController] Módulo actualizado con ID: {}", id);
                    return ResponseEntity.ok(moduloActualizado);
                },
                "No encontrado al actualizar ID " + id + ": {}",
                "Error actualizando módulo con ID " + id + ": {}"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        log.info("📋 [ModuleController] Recibida solicitud para eliminar módulo con ID: {}", id);
        return handle(
                () -> {
                    moduleService.eliminar(id);
                    log.info("✅ [ModuleController] Módulo eliminado con ID: {}", id);
                    return ResponseEntity.noContent().build();
                },
                "Módulo no encontrado al eliminar ID " + id + ": {}",
                "Error eliminando módulo con ID " + id + ": {}"
        );
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        log.info("📋 [ModuleController] Recibida solicitud para toggle activo/inactivo módulo con ID: {}", id);
        return handle(
                () -> {
                    moduleService.toggleActive(id);
                    log.info("✅ [ModuleController] Módulo toggle activo/inactivo con ID: {}", id);
                    return ResponseEntity.noContent().build();
                },
                "Módulo no encontrado al toggle ID " + id + ": {}",
                "Error toggle activo/inactivo módulo con ID " + id + ": {}"
        );
    }
}
