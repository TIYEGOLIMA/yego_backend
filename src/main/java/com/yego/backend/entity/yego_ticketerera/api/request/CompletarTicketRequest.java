package com.yego.backend.entity.yego_ticketerera.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO de request para completar un ticket en el sistema YEGO Ticketerera
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletarTicketRequest {
    
    @NotNull(message = "El ID del agente es obligatorio")
    private Long agentId;
    
    @Size(max = 1000, message = "Las notas no pueden exceder 1000 caracteres")
    private String notes; // OPCIONAL - puede ser null o vacío
}
