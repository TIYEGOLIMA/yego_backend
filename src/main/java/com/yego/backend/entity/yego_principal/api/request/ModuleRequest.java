package com.yego.backend.entity.yego_principal.api.request;

import com.yego.backend.entity.yego_principal.entities.Module;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String descripcion;
    
    @NotBlank(message = "La URL es obligatoria")
    private String url;
    
    private String estado;
    
    @Builder.Default
    private Boolean activo = true;
}
