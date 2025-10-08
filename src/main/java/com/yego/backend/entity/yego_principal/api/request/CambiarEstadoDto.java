package com.yego.backend.entity.yego_principal.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambiarEstadoDto {
    
    @NotNull(message = "El campo activo es requerido")
    private Boolean activo;
}
