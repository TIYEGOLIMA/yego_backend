package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.service.yego_principal.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para auditoría del sistema YEGO Principal
 * Equivalente a AuditController de NestJS
 */
@Slf4j
@RestController
@RequestMapping("/api/yego-principal/audit")
@RequiredArgsConstructor
public class AuditController {
    
    private final AuditService auditService;
    
    /**
     * Crear log de auditoría
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateAuditLogDto createAuditLogDto,
                                   Authentication authentication) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            AuditLogResponseDto auditLog = auditService.create(createAuditLogDto, userId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(auditLog);
            
        } catch (Exception e) {
            log.error("Error creando log de auditoría YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener logs de auditoría con filtros
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> findAll(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search) {
        
        try {
            AuditFilterDto filters = AuditFilterDto.builder()
                    .userId(userId)
                    .action(action)
                    .resource(resource)
                    .startDate(startDate != null ? LocalDate.parse(startDate).atStartOfDay() : null)
                    .endDate(endDate != null ? LocalDate.parse(endDate).atTime(23, 59, 59) : null)
                    .search(search)
                    .build();
            
            AuditLogPageDto result = auditService.findAll(page, limit, filters);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error obteniendo logs de auditoría YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener estadísticas de auditoría
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStats(@RequestParam(defaultValue = "30") Integer days) {
        try {
            AuditStatsDto stats = auditService.getStats(days);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de auditoría YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener actividad reciente
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> getRecentActivity(@RequestParam(defaultValue = "20") Integer limit) {
        try {
            List<AuditLogResponseDto> recentActivity = auditService.getRecentActivity(limit);
            return ResponseEntity.ok(recentActivity);
            
        } catch (Exception e) {
            log.error("Error obteniendo actividad reciente YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener logs de un usuario específico
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findByUser(@PathVariable Long userId,
                                       @RequestParam(defaultValue = "100") Integer limit) {
        try {
            List<AuditLogResponseDto> userLogs = auditService.findByUser(userId, limit);
            return ResponseEntity.ok(userLogs);
            
        } catch (Exception e) {
            log.error("Error obteniendo logs de usuario YEGO Principal {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener logs por acción específica
     */
    @GetMapping("/action/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findByAction(@PathVariable String action,
                                         @RequestParam(defaultValue = "100") Integer limit) {
        try {
            List<AuditLogResponseDto> actionLogs = auditService.findByAction(action, limit);
            return ResponseEntity.ok(actionLogs);
            
        } catch (Exception e) {
            log.error("Error obteniendo logs por acción YEGO Principal {}: {}", action, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener logs por recurso específico
     */
    @GetMapping("/resource/{resource}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findByResource(@PathVariable String resource,
                                           @RequestParam(defaultValue = "100") Integer limit) {
        try {
            List<AuditLogResponseDto> resourceLogs = auditService.findByResource(resource, limit);
            return ResponseEntity.ok(resourceLogs);
            
        } catch (Exception e) {
            log.error("Error obteniendo logs por recurso YEGO Principal {}: {}", resource, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener log específico por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        try {
            AuditLogResponseDto auditLog = auditService.findOne(id);
            return ResponseEntity.ok(auditLog);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo log de auditoría YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
}
