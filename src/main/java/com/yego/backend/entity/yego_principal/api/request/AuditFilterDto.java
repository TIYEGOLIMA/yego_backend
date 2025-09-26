package com.yego.backend.entity.yego_principal.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para filtros de auditoría del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditFilterDto {
    
    private Long userId;
    private String action;
    private String resource;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String search;
}

