package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta completa para usuarios del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseCompleteDto {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String role;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private Long moduleId;
    private LocalDateTime updatedAt;
    private Boolean requiereCambioPassword;
}

