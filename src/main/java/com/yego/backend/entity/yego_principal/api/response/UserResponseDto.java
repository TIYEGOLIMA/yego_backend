package com.yego.backend.entity.yego_principal.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuesta de usuarios del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {
    
    private Long id;
    private String username;
    private String email;
    private String name;
    private String lastName;
    private String dni;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private Long moduleId;
    
    // Información del rol
    private Long role;        // ID del rol
    private String roleName;  // Nombre del rol para mostrar

    private Long areaId;
    private String areaNombre;
}