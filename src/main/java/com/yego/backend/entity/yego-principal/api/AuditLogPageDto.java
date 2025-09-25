package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para paginación de logs de auditoría del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogPageDto {
    
    private List<AuditLogResponseDto> logs;
    private Long total;
    private Integer page;
    private Integer limit;
    private Integer totalPages;
    
    public Integer getTotalPages() {
        if (total == null || limit == null || limit == 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / limit);
    }
}
