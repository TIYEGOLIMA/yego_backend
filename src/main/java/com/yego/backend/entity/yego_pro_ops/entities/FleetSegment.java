package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Flota / segmento. Identifica una flota de Yango por su park_id.
 * Es la tabla "maestra" de categorías: cada vehículo pertenece a una flota.
 */
@Entity
@Table(name = "module_fleet_segments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "park_id", nullable = false, unique = true, length = 255)
    private String parkId;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by_id")
    private Long createdById;
}
