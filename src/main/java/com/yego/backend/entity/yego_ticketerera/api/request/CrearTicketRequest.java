package com.yego.backend.entity.yego_ticketerera.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de request para crear un ticket en el sistema YEGO Ticketerera
 */
@Data
public class CrearTicketRequest {
    
    @NotNull(message = "El optionId es requerido")
    private Long optionId;
    
    // userId ya no es requerido - se asigna automáticamente cuando un agente toma el ticket
    private Long userId;
    
    private String licenseNumber;

    private Long sedeId;
}
