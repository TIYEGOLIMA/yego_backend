package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento unificado de trazabilidad del vehículo (flota + documentos).
 * tipo: INGRESO | CAMBIO_FLOTA | DOC_CARGADO | DOC_ELIMINADO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTraceEvent {
    private String tipo;
    private String descripcion;
    private String usuario;
    private LocalDateTime fecha;
    private String flotaAnterior;
    private String flotaNuevo;
}
