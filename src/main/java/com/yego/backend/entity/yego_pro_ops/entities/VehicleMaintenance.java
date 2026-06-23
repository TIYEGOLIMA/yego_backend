package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_vehicle_maintenance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMaintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "yango_car_id", nullable = false, length = 255)
    private String yangoCarId;

    @Column(name = "tipo", nullable = false, length = 20)
    @Builder.Default
    private String tipo = "preventivo";

    @Column(name = "categoria", length = 100)
    private String categoria;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "kilometraje", precision = 12, scale = 2)
    private BigDecimal kilometraje;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "problema", columnDefinition = "TEXT")
    private String problema;

    @Column(name = "diagnostico", columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "solucion", columnDefinition = "TEXT")
    private String solucion;

    @Column(name = "taller", length = 200)
    private String taller;

    @Column(name = "responsable", length = 200)
    private String responsable;

    @Column(name = "costo", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costo = BigDecimal.ZERO;

    @Column(name = "archivo_url", columnDefinition = "TEXT")
    private String archivoUrl;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "completado";

    @Column(name = "proxima_fecha")
    private LocalDate proximaFecha;

    @Column(name = "proximo_km", precision = 12, scale = 2)
    private BigDecimal proximoKm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
