package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para auditoría del sistema YEGO Principal
 */
@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {
    
    private final AuditService auditService;
    
    /**
     * Obtener todos los logs de auditoría
     */
    @GetMapping
    public ResponseEntity<?> getAllAuditLogs(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) Long userId) {
        
        AuditFilterDto filters = AuditFilterDto.builder()
                .action(action)
                .resource(resource)
                .userId(userId)
                .build();
        
        AuditLogPageDto logs = auditService.findAll(page, size, filters);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Obtener logs de auditoría por usuario
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLogResponseDto>> getAuditLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "50") Integer limit) {
        List<AuditLogResponseDto> logs = auditService.findByUser(userId, limit);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Obtener logs de auditoría por acción
     */
    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLogResponseDto>> getAuditLogsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "50") Integer limit) {
        List<AuditLogResponseDto> logs = auditService.findByAction(action, limit);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Obtener estadísticas de auditoría
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getAuditStats(@RequestParam(defaultValue = "30") Integer days) {
        AuditStatsDto stats = auditService.getStats(days);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener actividad reciente
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AuditLogResponseDto>> getRecentActivity(
            @RequestParam(defaultValue = "20") Integer limit) {
        List<AuditLogResponseDto> logs = auditService.getRecentActivity(limit);
        return ResponseEntity.ok(logs);
    }
}