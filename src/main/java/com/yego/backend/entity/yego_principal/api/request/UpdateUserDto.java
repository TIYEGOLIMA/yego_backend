package com.yego.backend.entity.yego_principal.api.request;

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
    @Size(min = 2, max = 255, message = "El nombre de usuario debe tener entre 2 y 255 caracteres")
    private String username;

    @Email(message = "El formato del email no es válido")
    private String email;

    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    private String name;

    private Long roleId;

    @Size(max = 255, message = "El apellido no puede exceder 255 caracteres")
    private String lastName;

    @Size(min = 8, max = 12, message = "El documento debe tener entre 8 y 12 caracteres")
    private String dni;

    private Boolean active;

    private Long areaId;

    private Long sedeId;
}

