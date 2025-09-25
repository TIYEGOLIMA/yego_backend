package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para estadísticas de usuario del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDto {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String role;
    private LocalDateTime lastLogin;
    private Boolean active;
    private LocalDateTime createdAt;
}
