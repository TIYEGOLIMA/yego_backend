package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.service.yego_principal.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para importaciones del sistema YEGO Principal
 * Equivalente a ImportsController de NestJS
 */
@Slf4j
@RestController
@RequestMapping("/api/yego-principal/imports")
@RequiredArgsConstructor
public class ImportController {
    
    private final ImportService importService;
    
    /**
     * Subir archivo CSV para importación
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("userId") Long userId) {
        
        try {
            // Validar archivo CSV
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "El archivo no puede estar vacío"));
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Solo se permiten archivos CSV"));
            }
            
            UploadImportDto uploadImportDto = UploadImportDto.builder()
                    .type(type)
                    .filename(file.getOriginalFilename())
                    .build();
            
            ImportUploadResponseDto response = importService.uploadFile(file, uploadImportDto, userId);
            
            log.info("📁 Archivo YEGO Principal subido exitosamente: {} por usuario {}", 
                    file.getOriginalFilename(), userId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error subiendo archivo YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener todas las importaciones
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> findAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            List<ImportResponseDto> imports = importService.findAll(userId, startDate, endDate);
            return ResponseEntity.ok(imports);
            
        } catch (Exception e) {
            log.error("Error obteniendo importaciones YEGO Principal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener importación por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        try {
            ImportResponseDto importDto = importService.findOne(id);
            return ResponseEntity.ok(importDto);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo importación YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Obtener vista previa de importación
     */
    @GetMapping("/{id}/preview")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> getPreview(@PathVariable Long id) {
        try {
            ImportPreviewDto preview = importService.getPreview(id);
            return ResponseEntity.ok(preview);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo vista previa YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Procesar importación
     */
    @PostMapping("/{id}/process")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<?> processImport(@PathVariable Long id, 
                                          @Valid @RequestBody ProcessImportDto processImportDto) {
        try {
            ImportResponseDto result = importService.processImport(id, processImportDto);
            
            log.info("⚙️ Importación YEGO Principal procesada: ID {}", id);
            
            return ResponseEntity.ok(result);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error procesando importación YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
    
    /**
     * Eliminar importación
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        try {
            importService.remove(id);
            
            log.info("🗑️ Importación YEGO Principal eliminada: ID {}", id);
            
            return ResponseEntity.ok(Map.of("message", "Importación eliminada exitosamente"));
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error eliminando importación YEGO Principal {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
    }
}
