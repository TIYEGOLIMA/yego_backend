package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simple de usuario (id, nombre completo, areaId) para combos.
 * areaId permite en frontend no mostrar en combo de colaboradores a usuarios ya asignados a otra área.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleDto {
    private Long id;
    private String nombreCompleto;
    /** Área a la que pertenece el usuario (null si no tiene). Usado para filtrar combo de colaboradores. */
    private Long areaId;
}
