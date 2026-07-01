package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Vehículo cacheado desde Yango. Es el maestro local del auto.
 * Referencia su flota por segment_id (FK a module_fleet_segments.id).
 */
@Entity
@Table(name = "module_fleet_vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetVehicle {

    @Id
    @Column(name = "yango_car_id", length = 255)
    private String yangoCarId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_id", nullable = false)
    private FleetSegment segment;

    @Column(name = "number", length = 50)
    private String number;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "year")
    private Integer year;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "color_name", length = 100)
    private String colorName;

    @Column(name = "vin", length = 100)
    private String vin;

    @Column(name = "callsign", length = 100)
    private String callsign;

    @Column(name = "status_id", length = 50)
    private String statusId;

    @Column(name = "status_name", length = 100)
    private String statusName;

    @Column(name = "categories", columnDefinition = "TEXT")
    private String categories;

    @Column(name = "amenities", columnDefinition = "TEXT")
    private String amenities;

    @Column(name = "mileage")
    private Integer mileage;

    @Column(name = "rental")
    private Boolean rental;

    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    @Column(name = "modified_date", length = 50)
    private String modifiedDate;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "synced_at", nullable = false)
    @Builder.Default
    private LocalDateTime syncedAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
