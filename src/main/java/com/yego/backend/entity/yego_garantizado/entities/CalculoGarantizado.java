package com.yego.backend.entity.yego_garantizado.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad para la tabla module_guaranteed_calculations
 * Almacena el histórico de cálculos de garantizado por país, ciudad y semana
 */
@Entity
@Table(name = "module_guaranteed_calculations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculoGarantizado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "pais", nullable = false, length = 120)
    private String pais;

    @Column(name = "ciudad", nullable = false, length = 120)
    private String ciudad;

    @Column(name = "semana", nullable = false, length = 20)
    private String semana;

    // Datos con brandeo
    @Column(name = "viajes_con_brandeo", nullable = false)
    private Integer viajesConBrandeo;

    @Column(name = "bono_con_brandeo", nullable = false, precision = 10, scale = 2)
    private BigDecimal bonoConBrandeo;

    @Column(name = "garantizado_con_brandeo", nullable = false, precision = 10, scale = 2)
    private BigDecimal garantizadoConBrandeo;

    @Column(name = "horas_con_brandeo", nullable = false)
    private Integer horasConBrandeo;

    // Datos sin brandeo
    @Column(name = "viajes_sin_brandeo", nullable = false)
    private Integer viajesSinBrandeo;

    @Column(name = "bono_sin_brandeo", nullable = false, precision = 10, scale = 2)
    private BigDecimal bonoSinBrandeo;

    @Column(name = "garantizado_sin_brandeo", nullable = false, precision = 10, scale = 2)
    private BigDecimal garantizadoSinBrandeo;

    @Column(name = "horas_sin_brandeo", nullable = false)
    private Integer horasSinBrandeo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
        updatedAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    }
}

