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
@Table(name = "operational_trip_facts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalTripFact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "external_trip_id", nullable = false, length = 255, unique = true)
    private String externalTripId;

    @Column(name = "driver_id", nullable = false, length = 255)
    private String driverId;

    @Column(name = "vehicle_key", length = 255)
    private String vehicleKey;

    @Column(name = "vehicle_key_source", length = 32)
    private String vehicleKeySource;

    @Column(name = "vehicle_id", length = 255)
    private String vehicleId;

    @Column(name = "vehicle_plate", length = 32)
    private String vehiclePlate;

    @Column(name = "vehicle_plate_normalized", length = 32)
    private String vehiclePlateNormalized;

    @Column(name = "trip_status", length = 64)
    private String tripStatus;

    @Column(name = "booked_at")
    private LocalDateTime bookedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "source", nullable = false, length = 32)
    @Builder.Default
    private String source = "YANGO";

    @Column(name = "source_payload_hash", length = 128)
    private String sourcePayloadHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
