package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de rol con conteo de usuarios del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleWithUsersDto {
    private Long id;
    private String name;
    private String description;
    private Map<String, Object> permissions;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userCount;
}
