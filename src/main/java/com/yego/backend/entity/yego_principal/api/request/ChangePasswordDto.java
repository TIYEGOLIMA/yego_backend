package com.yego.backend.entity.yego_principal.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para cambio de contraseña del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordDto {
    
    @Size(min = 3, message = "El nombre de usuario debe tener al menos 3 caracteres")
    private String username;
    
    @NotBlank(message = "La contraseña actual es obligatoria")
    @Size(min = 6, message = "La contraseña actual debe tener al menos 6 caracteres")
    private String currentPassword;
    
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    private String newPassword;
    
    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    @Size(min = 8, message = "La confirmación de contraseña debe tener al menos 8 caracteres")
    private String confirmPassword;
}

