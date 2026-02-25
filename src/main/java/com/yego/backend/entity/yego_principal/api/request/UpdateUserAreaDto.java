package com.yego.backend.entity.yego_principal.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar solo el área de un usuario (asignar o quitar de un área).
 * areaId = null o 0 significa "quitar del área".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserAreaDto {
    private Long areaId;
}
