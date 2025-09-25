package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta para subida de archivos de importación del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportUploadResponseDto {
    
    private String message;
    private Long importId;
    private List<Object> preview;
    private Map<String, Object> errors;
    private Integer totalRows;
}
