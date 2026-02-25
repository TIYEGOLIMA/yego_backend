package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de colaborador de un área (usuario asignado al área).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColaboradorDto {
    private Long id;
    private String nombreCompleto;
    private String email;
    private String rol;
}
