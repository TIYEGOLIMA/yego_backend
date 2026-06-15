package com.yego.backend.entity.yego_api_externo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fleet_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FleetCache {

    @Id
    @Column(name = "park_id", length = 255)
    private String parkId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
