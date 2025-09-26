package com.yego.backend.entity.yego_principal.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad Configuration del sistema YEGO Principal
 * Equivalente a configuration.entity.ts de NestJS
 */
@Entity
@Table(name = "configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Configuration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String key;
    
    @Column(columnDefinition = "TEXT")
    private String value;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 50)
    private String category;
    
    @Column(length = 20)
    @Builder.Default
    private String type = "string";
    
    @Column(name = "created_at", nullable = false, updatable = false)
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

