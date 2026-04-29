package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Boolean active;
    private LocalDateTime lastLogin;
    private Boolean esJefe;
    private String nombreArea;
    private Boolean esSupervisor;
    private String nombreAreaSupervisor;
    private Boolean requirePasswordChange;
    /** Área principal del usuario (columna `users.area_id`). */
    private Long areaId;
    /** Nombre completo del área desde BD; null si no hay área o no existe la fila. */
    private String areaNombre;
    private Long sedeId;
    private String sedeNombre;
}

