package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para establecer valor de configuración del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetValueDto {
    
    @NotNull(message = "El valor es obligatorio")
    private Object value;
    
    @Pattern(regexp = "^(string|number|boolean|json)$", message = "El tipo debe ser: string, number, boolean o json")
    @Builder.Default
    private String type = "string";
}
