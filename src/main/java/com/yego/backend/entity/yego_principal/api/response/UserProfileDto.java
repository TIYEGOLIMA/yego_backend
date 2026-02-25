package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para perfil de usuario del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    
    private Long id;
    private String username;
    private String email;
    private String name;
    private String role;
    private String moduleId;
    private Boolean active;
    private LocalDateTime lastLogin;
    /** Si el usuario es jefe de un área (manager_id en tabla areas). */
    private Boolean esJefe;
    /** Nombre del área que gestiona (solo si esJefe). */
    private String nombreArea;
}

