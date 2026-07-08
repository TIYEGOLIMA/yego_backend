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
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shift_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false, length = 255)
    private String driverId;

    @Column(name = "vehicle_id", length = 255)
    private String vehicleId;

    @Column(name = "placa", length = 30)
    private String placa;

    @Column(name = "modelo", length = 255)
    private String modelo;

    @Column(name = "km_inicial")
    private Integer kmInicial;

    @Column(name = "km_final")
    private Integer kmFinal;

    @Column(name = "selfie_uri", columnDefinition = "TEXT")
    private String selfieUri;

    @Column(name = "car_photos", columnDefinition = "TEXT")
    private String carPhotos;

    @Column(name = "car_photos_cierre", columnDefinition = "TEXT")
    private String carPhotosCierre;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "mantenimiento_requerido")
    @Builder.Default
    private Boolean mantenimientoRequerido = false;

    @Column(name = "mantenimiento_descripcion", columnDefinition = "TEXT")
    private String mantenimientoDescripcion;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "total_trips")
    @Builder.Default
    private Integer totalTrips = 0;

    @Column(name = "total_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "total_cash", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCash = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "delete_reason", columnDefinition = "TEXT")
    private String deleteReason;
}
