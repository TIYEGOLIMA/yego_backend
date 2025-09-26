package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para configuraciones del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationResponseDto {
    
    private Long id;
    private String key;
    private String value;
    private String description;
    private String category;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

