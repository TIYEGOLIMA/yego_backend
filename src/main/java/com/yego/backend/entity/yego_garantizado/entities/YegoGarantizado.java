package com.yego.backend.entity.yego_garantizado.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "queue_garantizado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YegoGarantizado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "yeg_gara_id")
    private Long id;

    @Column(name = "yeg_gara_nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(name = "yeg_gara_numero_licencia", nullable = false)
    private String numeroLicencia;

    @Column(name = "yeg_gara_telefono")
    private String telefono;

    @Column(name = "yeg_gara_viajes")
    private Integer viajes;

    @Column(name = "yeg_gara_efectivo", precision = 10, scale = 2)
    private BigDecimal efectivo;

    @Column(name = "yeg_gara_pago_sin_efectivo", precision = 10, scale = 2)
    private BigDecimal pagoSinEfectivo;

    @Column(name = "yeg_gara_com_yango", precision = 10, scale = 2)
    private BigDecimal comYango;

    @Column(name = "yeg_gara_com_yego", precision = 10, scale = 2)
    private BigDecimal comYego;

    @Column(name = "yeg_gara_bo_sem_ant", precision = 10, scale = 2)
    private BigDecimal boSemAnt;

    @Column(name = "yeg_gara_bo_sem_act", precision = 10, scale = 2)
    private BigDecimal boSemAct;

    @Column(name = "yeg_gara_total", precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "yeg_gara_garantizado", precision = 10, scale = 2)
    private BigDecimal garantizado;

    @Column(name = "yeg_gara_diferencia", precision = 10, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "yeg_gara_semana")
    private String semana;

    @Column(name = "yeg_gara_viajes_actuales")
    private Integer viajesActuales;

    @Column(name = "yeg_gara_flota_id", nullable = false)
    private String flotaId;

    @Column(name = "yeg_gara_garantizado_valor", nullable = false)
    private String garantizadoValor;

    @Column(name = "yeg_gara_fecha_creacion", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    @Column(name = "yeg_gara_fecha_actualizacion")
    @UpdateTimestamp
    private LocalDateTime fechaActualizacion;

    @Column(name = "yeg_gara_activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "yeg_gara_estado_pago")
    private String estadoPago;

    @Column(name = "yeg_gara_usuario_pago_id")
    private Long usuarioPagoId;

    @Column(name = "yeg_gara_horas_trabajadas")
    private String horasTrabajadas;

    @Column(name = "yeg_gara_horas_trabajadas_entero")
    private Integer horasTrabajadasEntero;

    @Column(name = "yeg_gara_motivo_rechazo")
    private String motivoRechazo;
}
