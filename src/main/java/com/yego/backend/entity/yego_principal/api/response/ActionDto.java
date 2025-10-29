package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa una acción disponible para un módulo
 * Útil para formularios de creación/edición de roles
 * Los atributos coinciden con Permission entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionDto {

    private String action;
    private String name;
    private String description;
    private String module;
}

