package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO completo de respuesta de usuario del sistema YEGO Principal
 * Equivalente a UserResponseDto de NestJS (versión completa)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseCompleteDto {
    
    private Long id;
    
    private String username;
    
    private String email;
    
    private String dni;
    
    private String nombre;
    
    private String telefono;
    
    private String direccion;
    
    private LocalDate fechaNacimiento;
    
    private Boolean activo;
    
    private Boolean requiereCambioPassword;
    
    private List<RoleSummaryDto> roles;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleSummaryDto {
        private Long id;
        private String name;
        private String description;
    }
}
