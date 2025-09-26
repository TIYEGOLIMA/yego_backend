package com.yego.backend.entity.yego_principal.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para registro de usuarios del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDto {
    
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, message = "El nombre de usuario debe tener al menos 3 caracteres")
    private String username;
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    private String email;
    
    private String dni;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String telefono;
    
    private String direccion;
}

