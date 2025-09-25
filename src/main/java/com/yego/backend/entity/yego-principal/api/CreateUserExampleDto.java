package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO para ejemplo de creación de usuario en YEGO Principal
 * Equivalente a CreateUserExampleDto de NestJS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserExampleDto {
    
    @NotBlank(message = "El username es requerido")
    private String username;
    
    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe tener un formato válido")
    private String email;
    
    private String dni;
    
    @NotBlank(message = "El primer nombre es requerido")
    private String firstName;
    
    @NotBlank(message = "Los apellidos son requeridos")
    private String lastName;
    
    private String telefono;
    
    private String direccion;
    
    private List<Long> roleIds;
}
