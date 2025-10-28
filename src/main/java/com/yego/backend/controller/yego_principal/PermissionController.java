package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.response.PermissionResponseDto;
import com.yego.backend.service.yego_principal.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * Obtener todos los permisos activos
     */
    @GetMapping("/find-all-active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<List<PermissionResponseDto>> findAllActive() {
        List<PermissionResponseDto> permissions = permissionService.findAllActive();
        return ResponseEntity.ok(permissions);
    }
    
    /**
     * Obtener todos los permisos
     */
    @GetMapping("/find-all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<List<PermissionResponseDto>> findAll() {
        List<PermissionResponseDto> permissions = permissionService.findAll();
        return ResponseEntity.ok(permissions);
    }
    
    /**
     * Obtener permisos por módulo
     */
    @GetMapping("/module/{module}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<List<PermissionResponseDto>> findByModule(@PathVariable String module) {
        List<PermissionResponseDto> permissions = permissionService.findByModule(module);
        return ResponseEntity.ok(permissions);
    }
}