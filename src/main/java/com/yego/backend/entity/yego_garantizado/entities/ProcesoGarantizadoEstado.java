package com.yego.backend.entity.yego_garantizado.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad para la tabla module_guaranteed_process_status
 * Almacena el estado del último procesamiento para controlar el bloqueo del botón
 */
@Entity
@Table(name = "module_guaranteed_process_status", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoGarantizadoEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "ultimo_procesamiento", nullable = false)
    private LocalDateTime ultimoProcesamiento;

    @Column(name = "bloqueado", nullable = false)
    private Boolean bloqueado = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    }
}

