package com.yego.backend.entity.yego_principal.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA para usuarios del sistema YEGO Principal
 * Equivalente a User entity de TypeORM
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;
    
    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role", nullable = false)
    private Role role;
    
    @Column(name = "username", nullable = false, unique = true, length = 255)
    private String username;
    
    @Column(name = "module_id")
    private Long moduleId;

    @Column(name = "dni", length = 8, nullable = false, unique = true)
    private String dni;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Métodos helper para compatibilidad
    public Long getRoleId() {
        return role != null ? role.getId() : null;
    }
    
    public String getRoleName() {
        return role != null ? role.getName() : null;
    }
    
    public Set<Permission> getUserPermissions() {
        return new HashSet<>();
    }
}

