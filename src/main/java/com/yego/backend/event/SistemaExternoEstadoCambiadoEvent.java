package com.yego.backend.event;

import com.yego.backend.entity.yego_principal.entities.SistemaExterno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento cuando cambia el estado de un sistema externo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SistemaExternoEstadoCambiadoEvent {
    
    private Long sistemaId;
    private String nombre;
    private String url;
    private SistemaExterno.EstadoSistema estadoAnterior;
    private SistemaExterno.EstadoSistema estadoNuevo;
    private LocalDateTime timestamp;
    
    public static SistemaExternoEstadoCambiadoEvent fromSistema(SistemaExterno sistema, 
                                                               SistemaExterno.EstadoSistema estadoAnterior) {
        return SistemaExternoEstadoCambiadoEvent.builder()
                .sistemaId(sistema.getYegoSisExtId())
                .nombre(sistema.getYegoSisExtNombre())
                .url(sistema.getYegoSisExtUrl())
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(sistema.getYegoSisExtEstado())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
