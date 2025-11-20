package com.yego.backend.entity.yego_marketing_mensajes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entidad para la tabla yego_marketing_mensajes
 * Maneja los mensajes de marketing del sistema
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Entity
@Table(name = "module_marketing_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "mensaje", columnDefinition = "TEXT", nullable = false)
    private String mensaje;

    @Column(name = "modo", length = 50)
    private String modo;

    @Column(name = "tipo", length = 50)
    private String tipo;

    @Column(name = "archivo", columnDefinition = "TEXT")
    private String archivo;

    @Column(name = "whatsapp")
    private Boolean whatsapp;

    @Column(name = "yandex")
    private Boolean yandex;

    @Column(name = "dias_activos", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String diasActivos;

    @Column(name = "grupos", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String grupos;

    @Column(name = "flotas", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String flotas;

    @Column(name = "horas_especificas", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String horasEspecificas;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
        updatedAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
        if (activo == null) {
            activo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    }
}

