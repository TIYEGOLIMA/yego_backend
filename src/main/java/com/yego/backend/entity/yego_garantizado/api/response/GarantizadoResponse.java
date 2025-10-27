package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para respuesta de conductores garantizados
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GarantizadoResponse {
    
    private Long id;
    private String nombreCompleto;
    private String numeroLicencia;
    private String telefono;
    private Integer viajes;
    private BigDecimal efectivo;
    private BigDecimal pagoSinEfectivo;
    private BigDecimal comYango;
    private BigDecimal comYego;
    private BigDecimal boSemAnt;
    private BigDecimal boSemAct;
    private BigDecimal total;
    private BigDecimal garantizado;
    private BigDecimal diferencia;
    private String semana;
    private Integer viajesActuales;
    private String flotaId;
    private String garantizadoValor; // Garantizado o No Garantizado
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private Boolean activo;
    private String estadoPago;
    private Long usuarioPagoId;
    private String horasTrabajadas;
}
