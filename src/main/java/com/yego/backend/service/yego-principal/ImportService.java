package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Interfaz del servicio de importaciones del sistema YEGO Principal
 * Equivalente a ImportsService de NestJS
 */
public interface ImportService {
    
    /**
     * Subir archivo y crear importación
     */
    ImportUploadResponseDto uploadFile(MultipartFile file, UploadImportDto uploadImportDto, Long userId);
    
    /**
     * Obtener todas las importaciones con filtros opcionales
     */
    List<ImportResponseDto> findAll(Long userId, String startDate, String endDate);
    
    /**
     * Obtener importación por ID
     */
    ImportResponseDto findOne(Long id);
    
    /**
     * Obtener vista previa de importación
     */
    ImportPreviewDto getPreview(Long id);
    
    /**
     * Procesar importación
     */
    ImportResponseDto processImport(Long id, ProcessImportDto processImportDto);
    
    /**
     * Eliminar importación
     */
    void remove(Long id);
}
