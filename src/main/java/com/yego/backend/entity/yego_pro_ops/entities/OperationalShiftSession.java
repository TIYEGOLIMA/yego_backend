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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "operational_shift_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalShiftSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false, length = 255)
    private String driverId;

    @Column(name = "vehicle_key", length = 255)
    private String vehicleKey;

    @Column(name = "vehicle_key_source", length = 32)
    private String vehicleKeySource;

    @Column(name = "vehicle_id", length = 255)
    private String vehicleId;

    @Column(name = "vehicle_plate_normalized", length = 32)
    private String vehiclePlateNormalized;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "state", nullable = false, length = 64)
    private String state;

    @Column(name = "open_reason", nullable = false, length = 128)
    private String openReason;

    @Column(name = "close_reason", length = 128)
    private String closeReason;

    @Column(name = "first_trip_external_id", length = 255)
    private String firstTripExternalId;

    @Column(name = "last_trip_external_id", length = 255)
    private String lastTripExternalId;

    @Column(name = "trip_count", nullable = false)
    @Builder.Default
    private Integer tripCount = 0;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "confidence_level", length = 16)
    private String confidenceLevel;

    @Column(name = "needs_review", nullable = false)
    @Builder.Default
    private Boolean needsReview = false;

    @Column(name = "review_reason", length = 128)
    private String reviewReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
