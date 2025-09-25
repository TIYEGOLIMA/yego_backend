package com.yego.backend.entity.yego_principal.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDto {
    @Size(min = 3, max = 255, message = "El nombre de usuario debe tener entre 3 y 255 caracteres")
    private String username;

    @Email(message = "El formato del email no es válido")
    private String email;

    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    private String name;

    @Size(max = 255, message = "El rol no puede exceder 255 caracteres")
    private String role;

    private Boolean active;
    private Long moduleId;
}
