package com.yego.backend.entity.yego_principal.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear módulos del sistema YEGO Principal
 */
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

    /** Clave de pantalla en el frontend (ej. YEGO_GANTT). Opcional al crear; recomendado para rutas dinámicas. */
    private String codigo;
    
    private String estado;
    
    @NotBlank(message = "El icono es obligatorio")
    private String icono;
    
    private Long grupoId;
    
    @Builder.Default
    private Boolean activo = true;
}
