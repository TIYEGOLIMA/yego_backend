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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "operational_shift_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalShiftEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "operational_shift_session_id")
    private UUID operationalShiftSessionId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "driver_id", length = 255)
    private String driverId;

    @Column(name = "vehicle_key", length = 255)
    private String vehicleKey;

    @Column(name = "external_trip_id", length = 255)
    private String externalTripId;

    @Column(name = "reason", length = 128)
    private String reason;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
