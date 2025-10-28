package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simple para listar roles (solo id y name)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleSimpleDto {
    private Long id;
    private String name;
}
