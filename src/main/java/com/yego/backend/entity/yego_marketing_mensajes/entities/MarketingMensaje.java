package com.yego.backend.entity.yego_marketing_mensajes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "module_marketing_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingMensaje {

    private static final ZoneId ZONE_LIMA = ZoneId.of("America/Lima");

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

    @Column(name = "whatsapp", nullable = false)
    private Boolean whatsapp = false;

    @Column(name = "yandex", nullable = false)
    private Boolean yandex = false;

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
        LocalDateTime now = LocalDateTime.now(ZONE_LIMA);
        createdAt = now;
        updatedAt = now;
        if (activo == null) {
            activo = true;
        }
        if (whatsapp == null) {
            whatsapp = false;
        }
        if (yandex == null) {
            yandex = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZONE_LIMA);
    }
}
