package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_vehicle_incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "yango_car_id", nullable = false, length = 255)
    private String yangoCarId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "tipo", nullable = false, length = 100)
    private String tipo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "conductor", length = 200)
    private String conductor;

    @Column(name = "monto_dano", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montoDano = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "reportado";

    @Column(name = "evidencias", columnDefinition = "TEXT")
    private String evidencias;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
