package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.ModuleRequest;
import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import com.yego.backend.service.yego_principal.ModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@Slf4j
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping
    public ResponseEntity<List<ModuleResponse>> obtenerTodos() {
        log.info("📋 [ModuleController] Recibida solicitud para obtener todos los módulos");
        try {
            List<ModuleResponse> modulos = moduleService.obtenerTodos();
            log.info("✅ [ModuleController] Devueltos {} módulos", modulos.size());
            return ResponseEntity.ok(modulos);
        } catch (Exception e) {
            log.error("❌ [ModuleController] Error obteniendo módulos: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ModuleResponse>> obtenerActivos() {
        log.info("📋 [ModuleController] Recibida solicitud para obtener módulos activos");
        try {
            List<ModuleResponse> modulos = moduleService.obtenerActivos();
            log.info("✅ [ModuleController] Devueltos {} módulos activos", modulos.size());
            return ResponseEntity.ok(modulos);
        } catch (Exception e) {
            log.error("❌ [ModuleController] Error obteniendo módulos activos: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ModuleResponse> obtenerPorId(@PathVariable Long id) {
        log.info("📋 [ModuleController] Recibida solicitud para obtener módulo por ID: {}", id);
        try {
            ModuleResponse modulo = moduleService.obtenerPorId(id);
            log.info("✅ [ModuleController] Devuelto módulo con ID: {}", id);
            return ResponseEntity.ok(modulo);
        } catch (Exception e) {
            log.error("❌ [ModuleController] Error obteniendo módulo por ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<ModuleResponse> crear(@Valid @RequestBody ModuleRequest request) {
        log.info("📋 [ModuleController] Recibida solicitud para crear módulo: {}", request.getNombre());
        try {
            ModuleResponse nuevoModulo = moduleService.crear(request);
            log.info("✅ [ModuleController] Módulo creado con ID: {}", nuevoModulo.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoModulo);
        } catch (Exception e) {
            log.error("❌ [ModuleController] Error creando módulo: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModuleResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        log.info("📋 [ModuleController] Recibida solicitud para actualizar módulo con ID: {}", id);
        try {
            ModuleResponse moduloActualizado = moduleService.actualizar(id, request);
            log.info("✅ [ModuleController] Módulo actualizado con ID: {}", id);
            return ResponseEntity.ok(moduloActualizado);
        } catch (Exception e) {
            log.error("❌ [ModuleController] Error actualizando módulo con ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("📋 [ModuleController] Recibida solicitud para eliminar módulo con ID: {}", id);
        try {
            moduleService.eliminar(id);
            log.info("[ModuleController] Módulo eliminado con ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("[ModuleController] Error eliminando módulo con ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<Void> toggleActive(@PathVariable Long id) {
        log.info("📋 [ModuleController] Recibida solicitud para toggle activo/inactivo módulo con ID: {}", id);
        try {
            moduleService.toggleActive(id);
            log.info("✅ [ModuleController] Módulo toggle activo/inactivo con ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("❌ [ModuleController] Error toggle activo/inactivo módulo con ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
