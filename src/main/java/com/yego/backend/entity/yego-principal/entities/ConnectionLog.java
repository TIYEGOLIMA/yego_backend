package com.yego.backend.entity.yego_principal.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA para logs de conexión del sistema YEGO Principal
 * Equivalente a ConnectionLog entity de TypeORM
 */
@Entity
@Table(name = "connection_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;
    
    @Column(name = "session_id", insertable = false, updatable = false)
    private Long sessionId;
    
    @Column(name = "connection_type", length = 50)
    private String connectionType;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;
    
    @Column(name = "location", length = 255)
    private String location;
    
    @Column(name = "role_name", length = 100)
    private String roleName;
    
    @Column(name = "action", length = 100)
    private String action;
    
    @Column(name = "status", length = 50)
    private String status;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
