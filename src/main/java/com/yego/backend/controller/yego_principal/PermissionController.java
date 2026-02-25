package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.CreatePermissionDto;
import com.yego.backend.entity.yego_principal.api.request.UpdatePermissionDto;
import com.yego.backend.entity.yego_principal.api.response.PermissionResponseDto;
import com.yego.backend.service.yego_principal.PermissionService;
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

/**
 * Controlador REST para permisos del sistema YEGO Principal
 */
@Slf4j
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * Ejecuta una acción y devuelve ResponseEntity. 404 y 500 con mensaje en body.
     */
    private ResponseEntity<?> handle(Supplier<ResponseEntity<?>> action, String notFoundLog, String errorLog) {
        try {
            return action.get();
        } catch (EntityNotFoundException e) {
            log.warn("⚠️ [PermissionController] {}", notFoundLog, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("⚠️ [PermissionController] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [PermissionController] {}", errorLog, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Obtener todos los permisos (activos e inactivos) para la pantalla de gestión
     */
    @GetMapping("/find-all")
    public ResponseEntity<List<PermissionResponseDto>> findAll() {
        return ResponseEntity.ok(permissionService.findAll());
    }

    /**
     * Crear nuevo permiso
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreatePermissionDto createPermissionDto) {
        log.info("📋 [PermissionController] POST crear permiso: {}", createPermissionDto.getName());
        return handle(
                () -> {
                    PermissionResponseDto permission = permissionService.create(createPermissionDto);
                    log.info("✅ [PermissionController] Permiso creado: {}", permission.getName());
                    return ResponseEntity.status(HttpStatus.CREATED).body(permission);
                },
                "N/A",
                "Error creando permiso: {}"
        );
    }

    /**
     * Actualizar permiso
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                   @Valid @RequestBody UpdatePermissionDto updatePermissionDto) {
        log.info("📋 [PermissionController] PUT actualizar permiso ID: {}", id);
        return handle(
                () -> {
                    PermissionResponseDto permission = permissionService.update(id, updatePermissionDto);
                    log.info("✅ [PermissionController] Permiso actualizado: {}", permission.getName());
                    return ResponseEntity.ok(permission);
                },
                "Permiso no encontrado: {}",
                "Error actualizando permiso: {}"
        );
    }

    /**
     * Eliminar permiso (soft delete - desactivar)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        log.info("📋 [PermissionController] DELETE permiso ID: {}", id);
        return handle(
                () -> {
                    permissionService.remove(id);
                    log.info("✅ [PermissionController] Permiso desactivado: {}", id);
                    return ResponseEntity.noContent().build();
                },
                "Permiso no encontrado: {}",
                "Error eliminando permiso: {}"
        );
    }
}