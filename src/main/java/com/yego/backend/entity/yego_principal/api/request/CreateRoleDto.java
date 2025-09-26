package com.yego.backend.entity.yego_principal.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleDto {
    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre del rol debe tener entre 2 y 50 caracteres")
    private String name;

    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String description;

    private Map<String, Object> permissions;

    @Builder.Default
    private Boolean active = true;
}

