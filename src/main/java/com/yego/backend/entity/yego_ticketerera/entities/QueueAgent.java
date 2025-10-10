package com.yego.backend.entity.yego_ticketerera.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Entidad QueueAgent del sistema YEGO Ticketerera
 * Representa un agente en la cola de atención
 */
@Entity
@Table(name = "queue_agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueAgent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "module_id", nullable = false)
    private Long moduleId;
    
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "OCUPADO";
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        // Usar zona horaria de Perú (America/Lima)
        createdAt = LocalDateTime.now(ZoneId.of("America/Lima"));
        if (isActive == null) {
            isActive = true;
        }
        if (status == null) {
            status = "OCUPADO";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        // Usar zona horaria de Perú (America/Lima)
        updatedAt = LocalDateTime.now(ZoneId.of("America/Lima"));
    }
}
