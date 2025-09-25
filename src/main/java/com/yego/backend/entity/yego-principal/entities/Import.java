package com.yego.backend.entity.yego_principal.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad Import del sistema YEGO Principal
 * Equivalente a import.entity.ts de NestJS
 */
@Entity
@Table(name = "imports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Import {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(nullable = false, length = 255)
    private String filename;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ImportStatus status = ImportStatus.PENDING;
    
    @Column(name = "total_rows")
    @Builder.Default
    private Integer totalRows = 0;
    
    @Column(name = "processed_rows")
    @Builder.Default
    private Integer processedRows = 0;
    
    @Column(name = "success_rows")
    @Builder.Default
    private Integer successRows = 0;
    
    @Column(name = "error_rows")
    @Builder.Default
    private Integer errorRows = 0;
    
    @Column(columnDefinition = "jsonb")
    private String errors;
    
    @Column(columnDefinition = "jsonb")
    private List<Object> preview;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ImportType type;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum ImportStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
    
    public enum ImportType {
        USERS, ROLES, PERMISSIONS
    }
}
