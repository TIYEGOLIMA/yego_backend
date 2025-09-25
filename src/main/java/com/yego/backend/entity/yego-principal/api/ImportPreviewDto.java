package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO para vista previa de importaciones del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportPreviewDto {
    
    private Long id;
    private String filename;
    private String type;
    private Integer totalRows;
    private List<Object> preview;
    private Map<String, Object> errors;
}
