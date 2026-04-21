package com.yego.backend.entity.yego_ticketerera.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "dispositivos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispositivo {

    public enum TipoDispositivo {
        TABLET_PRINCIPAL, TABLET, TV
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TipoDispositivo type;

    @Column(name = "sede_id", nullable = false)
    private Long sedeId;

    @Column(name = "module_id")
    private Long moduleId;

    @Column(name = "access_token", nullable = false, unique = true, length = 255)
    private String accessToken;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneId.of("America/Lima"));
        if (active == null) active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.of("America/Lima"));
    }
}
