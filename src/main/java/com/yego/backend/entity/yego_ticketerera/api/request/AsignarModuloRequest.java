package com.yego.backend.entity.yego_ticketerera.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO de request para asignar un módulo de atención a un usuario en el sistema YEGO Ticketerera
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignarModuloRequest {
    
    @NotNull(message = "El userId es obligatorio")
    private Long userId;
    
    @NotNull(message = "El moduleId es obligatorio")
    private Long moduleId;
}
