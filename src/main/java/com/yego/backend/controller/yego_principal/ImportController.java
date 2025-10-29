package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controlador REST para importaciones del sistema YEGO Principal
 */
@Slf4j
@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {
    
    private final ImportService importService;
    
    /**
     * Obtener todas las importaciones
     */
    @GetMapping
    public ResponseEntity<List<ImportResponseDto>> findAll(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<ImportResponseDto> imports = importService.findAll(userId, startDate, endDate);
        return ResponseEntity.ok(imports);
    }
    
    /**
     * Obtener importación por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        ImportResponseDto importDto = importService.findOne(id);
        return ResponseEntity.ok(importDto);
    }
    
    /**
     * Subir archivo para importación
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                       @Valid @RequestBody UploadImportDto uploadImportDto,
                                       Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ImportUploadResponseDto response = importService.uploadFile(file, uploadImportDto, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener preview de importación
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<?> getPreview(@PathVariable Long id) {
        ImportPreviewDto preview = importService.getPreview(id);
        return ResponseEntity.ok(preview);
    }
    
    /**
     * Procesar importación
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<?> processImport(@PathVariable Long id,
                                          @Valid @RequestBody ProcessImportDto processImportDto) {
        ImportResponseDto response = importService.processImport(id, processImportDto);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancelar importación
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelImport(@PathVariable Long id) {
        importService.remove(id);
        return ResponseEntity.ok().build();
    }
}