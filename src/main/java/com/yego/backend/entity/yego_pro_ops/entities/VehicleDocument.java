package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_vehicle_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "yango_car_id", nullable = false, length = 255)
    private String yangoCarId;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "nombre", length = 200)
    private String nombre;

    @Column(name = "fecha_vigente")
    private LocalDate fechaVigente;

    @Column(name = "archivo_url", columnDefinition = "TEXT")
    private String archivoUrl;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "vigente";

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "eliminado", nullable = false)
    @Builder.Default
    private Boolean eliminado = false;

    @Column(name = "deleted_by_id")
    private Long deletedById;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
