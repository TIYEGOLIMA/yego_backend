package com.yego.backend.entity.yego_principal.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para subir archivos de importación del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadImportDto {
    
    @NotBlank(message = "El tipo de importación es obligatorio")
    @Pattern(regexp = "^(users|roles|permissions)$", message = "El tipo debe ser: users, roles o permissions")
    private String type;
    
    @NotBlank(message = "El nombre del archivo es obligatorio")
    private String filename;
}

