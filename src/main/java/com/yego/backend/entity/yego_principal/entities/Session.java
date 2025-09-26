package com.yego.backend.entity.yego_principal.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/**
 * Entidad JPA para sesiones del sistema YEGO Principal
 * Equivalente a Session entity de TypeORM
 */
@Entity
@Table(name = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;
    
    @Column(name = "ip_address")
    @JdbcTypeCode(SqlTypes.INET)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "device", length = 100)
    private String device;
    
    @Column(name = "browser", length = 100)
    private String browser;
    
    @Column(name = "operating_system", length = 100)
    private String operatingSystem;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "region", length = 100)
    private String region;
    
    @Column(name = "country", length = 100)
    private String country;
    
    @Column(name = "country_code", length = 10)
    private String countryCode;
    
    @Column(name = "latitude", precision = 10)
    private Double latitude;
    
    @Column(name = "longitude", precision = 10)
    private Double longitude;
    
    @Column(name = "timezone", length = 50)
    private String timezone;
    
    @Column(name = "isp", length = 200)
    private String isp;
    
    @Column(name = "organization", length = 200)
    private String organization;
    
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

