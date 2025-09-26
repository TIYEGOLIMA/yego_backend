package com.yego.backend.entity.yego_principal.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para procesar importaciones del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessImportDto {
    
    private List<Long> rowIds;
    
    @Builder.Default
    private Boolean skipValidation = false;
}

