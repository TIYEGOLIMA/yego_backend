package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_weekly_billing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturacionSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "driver_id", nullable = false, length = 255)
    private String driverId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "total_viajes")
    private Integer totalViajes;

    @Column(name = "viajes_validos")
    private Integer viajesValidos;

    @Column(name = "horas_trabajo")
    private Double horasTrabajo;

    @Column(name = "monto_total_producido", precision = 12, scale = 2)
    private BigDecimal montoTotalProducido;

    @Column(name = "comision_app", precision = 12, scale = 2)
    private BigDecimal comisionApp;

    @Column(name = "monto_neto", precision = 12, scale = 2)
    private BigDecimal montoNeto;

    @Column(name = "km_recorrido", precision = 10, scale = 2)
    private BigDecimal kmRecorrido;

    @Column(name = "gasto_combustible", precision = 10, scale = 2)
    private BigDecimal gastoCombustible;

    @Column(name = "bono_yango", precision = 12, scale = 2)
    private BigDecimal bonoYango;

    @Column(name = "gasto_mantenimiento", precision = 10, scale = 2)
    private BigDecimal gastoMantenimiento;

    @Column(name = "produccion_bonificable", precision = 12, scale = 2)
    private BigDecimal produccionBonificable;

    @Column(name = "bono_adic_viajes", precision = 10, scale = 2)
    private BigDecimal bonoAdicViajes;

    @Column(name = "bono", precision = 12, scale = 2)
    private BigDecimal bono;

    @Column(name = "porcentaje_pago")
    private Double porcentajePago;

    @Column(name = "pago", precision = 12, scale = 2)
    private BigDecimal pago;

    @Column(name = "bonificacion", precision = 10, scale = 2)
    private BigDecimal bonificacion;

    @Column(name = "garantia", precision = 10, scale = 2)
    private BigDecimal garantia;

    @Column(name = "descuento", precision = 10, scale = 2)
    private BigDecimal descuento;

    @Column(name = "general", length = 500)
    private String general;

    @Column(name = "pago_total", precision = 12, scale = 2)
    private BigDecimal pagoTotal;

    @Column(name = "bonificacion_empresa", precision = 12, scale = 2)
    private BigDecimal bonificacionEmpresa;

    @Column(name = "pago_total_final", precision = 12, scale = 2)
    private BigDecimal pagoTotalFinal;

    @Column(name = "total_adelantos", precision = 12, scale = 2)
    private BigDecimal totalAdelantos;

    @Column(name = "pago_total_con_adelantos", precision = 12, scale = 2)
    private BigDecimal pagoTotalConAdelantos;

    @Column(name = "utilidad", precision = 12, scale = 2)
    private BigDecimal utilidad;

    @Column(name = "utilidad_por_viaje", precision = 10, scale = 2)
    private BigDecimal utilidadPorViaje;

    @Column(name = "pago_por_viaje", precision = 10, scale = 2)
    private BigDecimal pagoPorViaje;

    @Column(name = "dias_trabajados")
    private Integer diasTrabajados;

    @Column(name = "dias_liquidados")
    private Integer diasLiquidados;

    @Column(name = "turno", length = 10)
    private String turno;

    @Column(name = "estado", length = 15, nullable = false)
    @Builder.Default
    private String estado = "pendiente";

    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
