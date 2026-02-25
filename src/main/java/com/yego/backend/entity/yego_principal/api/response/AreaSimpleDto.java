package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simple de área (id, name) para combos y formularios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaSimpleDto {
    private Long id;
    private String name;
}
