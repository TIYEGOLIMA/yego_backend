package com.yego.backend.event;

import com.yego.backend.entity.yego_principal.entities.SistemaExterno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento cuando se verifica el estado de un sistema externo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SistemaExternoVerificadoEvent {
    
    private Long sistemaId;
    private String nombre;
    private String url;
    private SistemaExterno.EstadoSistema estado;
    private LocalDateTime timestamp;
    private Boolean exitoso;
    private String mensaje;
    
    public static SistemaExternoVerificadoEvent fromSistema(SistemaExterno sistema, 
                                                           Boolean exitoso,
                                                           String mensaje) {
        return SistemaExternoVerificadoEvent.builder()
                .sistemaId(sistema.getYegoSisExtId())
                .nombre(sistema.getYegoSisExtNombre())
                .url(sistema.getYegoSisExtUrl())
                .estado(sistema.getYegoSisExtEstado())
                .timestamp(LocalDateTime.now())
                .exitoso(exitoso)
                .mensaje(mensaje)
                .build();
    }
}
