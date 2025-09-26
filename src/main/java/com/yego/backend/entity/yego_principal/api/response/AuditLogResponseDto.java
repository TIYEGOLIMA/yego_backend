package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para logs de auditoría del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDto {
    
    private Long id;
    private Long userId;
    private String action;
    private String resource;
    private String resourceId;
    private String details;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private AuditUserDto user;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditUserDto {
        private Long id;
        private String username;
        private String name;
        private String email;
    }
}

