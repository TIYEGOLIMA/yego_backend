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
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "session_id")
    private Long sessionId;
    
    @Column(name = "action", nullable = false, length = 20)
    private String action;
    
    @Column(name = "ip_address")
    @JdbcTypeCode(SqlTypes.INET)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
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
    
    @Column(name = "session_duration")
    private Integer sessionDuration;
    
    @Column(name = "role_name", length = 50)
    private String roleName;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

