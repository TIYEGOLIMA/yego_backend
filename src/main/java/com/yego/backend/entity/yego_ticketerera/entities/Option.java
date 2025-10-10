package com.yego.backend.entity.yego_ticketerera.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Entidad Option del sistema YEGO Ticketerera
 * Representa las opciones disponibles en el sistema de tickets
 */
@Entity
@Table(name = "options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Option {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column
    private String description;
    
    @Column(name = "parent_id")
    private Long parentId; // Para opciones anidadas
    
    @Column(name = "module_id")
    private Long moduleId; // Relación con módulos de atención
    
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        // Usar zona horaria de Perú (America/Lima)
        createdAt = LocalDateTime.now(ZoneId.of("America/Lima"));
        if (active == null) {
            active = true;
        }
        if (priority == null) {
            priority = 1;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        // Usar zona horaria de Perú (America/Lima)
        updatedAt = LocalDateTime.now(ZoneId.of("America/Lima"));
    }
}
