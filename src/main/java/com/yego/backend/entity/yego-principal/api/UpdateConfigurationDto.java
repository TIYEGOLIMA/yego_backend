package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Pattern;

/**
 * DTO para actualizar configuraciones del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConfigurationDto {
    
    private String value;
    
    private String description;
    
    private String category;
    
    @Pattern(regexp = "^(string|number|boolean|json)$", message = "El tipo debe ser: string, number, boolean o json")
    private String type;
}
