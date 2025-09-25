package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para importaciones del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponseDto {
    
    private Long id;
    private Long userId;
    private String filename;
    private String status; // pending, processing, completed, failed
    private Integer totalRows;
    private Integer processedRows;
    private Integer successRows;
    private Integer errorRows;
    private String errors;
    private List<Object> preview;
    private String type; // users, roles, permissions
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ImportUserDto user;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportUserDto {
        private Long id;
        private String name;
        private String username;
        private String email;
    }
}
