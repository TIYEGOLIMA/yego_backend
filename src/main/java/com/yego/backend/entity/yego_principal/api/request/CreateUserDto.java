package com.yego.backend.entity.yego_principal.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserDto {
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 2, max = 255, message = "El nombre de usuario debe tener entre 2 y 255 caracteres")
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    private String name;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 255, message = "El apellido no puede exceder 255 caracteres")
    private String lastName;

    @NotNull(message = "El rol es obligatorio")
    private Long roleId;

    @Builder.Default
    private Boolean active = true;

    @NotBlank(message = "El documento es obligatorio")
    @Size(min = 8, max = 12, message = "El documento debe tener entre 8 y 12 caracteres")
    private String dni;

    private Long sedeId;
}

