package com.yego.backend.entity.yego_garantizado.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad para la tabla module_guaranteed_locations
 * Maneja los países y ciudades del sistema de garantizado
 * 
 */
@Entity
@Table(name = "module_guaranteed_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "moneda", length = 3)
    private String moneda;

    @Column(name = "simbolo_moneda", length = 5)
    private String simboloMoneda;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "nivel", nullable = false, length = 10)
    private String nivel;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
        updatedAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    }
}

