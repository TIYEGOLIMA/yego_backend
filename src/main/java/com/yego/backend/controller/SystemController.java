package com.yego.backend.controller;

import com.yego.backend.entity.yego_principal.api.response.SystemStatusDto;
import com.yego.backend.service.yego_garantizado.SystemStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador REST para estado del sistema
 */
@RestController
@RequestMapping("/api/system")
@Slf4j
public class SystemController {
    
    private final SystemStatusService systemStatusService;

    public SystemController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Backend Garantizado");
        return ResponseEntity.ok(response);
    }
    /**
     * Obtener estado del sistema
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> systemStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("activo", systemStatusService.isSystemActive());
        response.put("status", systemStatusService.getCurrentStatus());
        response.put("proximaActivacion", systemStatusService.getNextActivationTime());
        response.put("proximaDesactivacion", systemStatusService.getNextDeactivationTime());
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
